import { describe, expect, it } from "vitest";
import { buildServer } from "../src/api/server.js";
import { loadConfig } from "../src/config.js";
import { mergeObservation } from "../src/domain/consensus.js";
import { RetentionJob, SNAPSHOT_BUCKET_MS, SnapshotJob } from "../src/jobs/snapshots.js";
import { MemoryStore } from "../src/store/memoryStore.js";

const rules = { confirmGapMs: 60_000, activeWindowMs: 6 * 3600_000 };

function listing(key: string, priceCents: number, seller: string, seenAt: number) {
  return mergeObservation(
    undefined,
    { key, baseId: `minecraft:${key}`, displayName: key, count: 1, priceCents, sellerHash: seller.padStart(32, "0"), expiresInSeconds: 80_000, slot: 0 },
    "c1",
    seenAt,
    rules,
  );
}

describe("snapshot job", () => {
  it("writes one bucket per active key and serves it as history", async () => {
    const store = new MemoryStore();
    const start = Date.parse("2026-11-05T12:00:00Z");
    await store.putListings([listing("emerald", 1_000, "a", start), listing("emerald", 1_200, "b", start), listing("tnt", 5_000, "c", start)]);
    const job = new SnapshotJob(store, rules);
    expect(await job.run(start + 1_000)).toBe(2);
    expect(await job.run(start + SNAPSHOT_BUCKET_MS + 1_000)).toBe(2);

    const app = buildServer({ config: loadConfig({}), store, now: () => start + SNAPSHOT_BUCKET_MS + 2_000 });
    const response = await app.inject({ method: "GET", url: "/v1/items/emerald/history?range=1h" });
    expect(response.statusCode).toBe(200);
    const points = response.json().points;
    expect(points).toHaveLength(2);
    expect(points[0].lowestUnitCents).toBe(1_000);
    expect(points[0].medianUnitCents).toBe(1_000);
    expect(points[1].bucketTs - points[0].bucketTs).toBe(SNAPSHOT_BUCKET_MS);

    const bad = await app.inject({ method: "GET", url: "/v1/items/emerald/history?range=2y" });
    expect(bad.statusCode).toBe(400);
  });
});

describe("retention job", () => {
  it("removes old observations and stale listings", async () => {
    const store = new MemoryStore();
    const day = 24 * 3600_000;
    const now = Date.parse("2026-11-05T12:00:00Z");
    await store.putListings([listing("emerald", 1_000, "a", now - 100 * day), listing("tnt", 5_000, "b", now - day)]);
    await store.recordObservations("c1", ["x"], now - 40 * day);
    await store.recordObservations("c1", ["y"], now - day);
    const result = await new RetentionJob(store, 30, 90).run(now);
    expect(result).toEqual({ observations: 1, listings: 1 });
    expect((await store.activeListings(0)).map((r) => r.key)).toEqual(["tnt"]);
  });
});
