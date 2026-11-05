import { describe, expect, it } from "vitest";
import { buildServer } from "../src/api/server.js";
import { loadConfig } from "../src/config.js";
import { MemoryStore } from "../src/store/memoryStore.js";

const CLIENT_A = "11111111-1111-4111-8111-111111111111";
const CLIENT_B = "22222222-2222-4222-8222-222222222222";

function batch(clientId: string, observedAt: string, priceCents = 80_000_000, expiresInSeconds = 30_000) {
  return {
    clientId,
    clientVersion: "0.0.1",
    mcVersion: "1.21.5",
    observedAt,
    source: "ah_list",
    listings: [
      {
        key: "nether_star",
        baseId: "minecraft:nether_star",
        displayName: "Nether Star",
        count: 1,
        priceCents,
        sellerHash: "ab".repeat(16),
        expiresInSeconds,
        slot: 0,
      },
    ],
  };
}

describe("api", () => {
  const start = Date.parse("2026-11-05T12:00:00Z");

  function server() {
    let clock = start;
    const app = buildServer({ config: loadConfig({}), store: new MemoryStore(), now: () => clock });
    return { app, advance: (ms: number) => (clock += ms), time: () => clock };
  }

  it("serves meta with fees and flags", async () => {
    const { app } = server();
    const response = await app.inject({ method: "GET", url: "/v1/meta" });
    expect(response.statusCode).toBe(200);
    const body = response.json();
    expect(body.fees.auctionListingFeePercent).toBe(1);
    expect(body.flags.sellmacro).toBe(false);
  });

  it("rejects malformed observations", async () => {
    const { app } = server();
    const response = await app.inject({ method: "POST", url: "/v1/observations", payload: { clientId: "x" } });
    expect(response.statusCode).toBe(400);
  });

  it("ingests, confirms with a second client and serves prices", async () => {
    const { app, advance, time } = server();
    const first = await app.inject({
      method: "POST",
      url: "/v1/observations",
      payload: batch(CLIENT_A, new Date(time()).toISOString()),
    });
    expect(first.statusCode).toBe(200);
    expect(first.json()).toEqual({ accepted: 1, newListings: 1, confirmed: 0 });

    let prices = await app.inject({ method: "POST", url: "/v1/prices/batch", payload: { keys: ["nether_star", "dragon_egg"] } });
    expect(prices.json().prices.nether_star.confirmed).toBe(false);
    expect(prices.json().prices.dragon_egg).toBeNull();

    advance(7_000);
    const second = await app.inject({
      method: "POST",
      url: "/v1/observations",
      payload: batch(CLIENT_B, new Date(time()).toISOString(), 80_000_000, 30_000 - 6),
    });
    expect(second.json()).toEqual({ accepted: 1, newListings: 0, confirmed: 1 });

    prices = await app.inject({ method: "POST", url: "/v1/prices/batch", payload: { keys: ["nether_star"] } });
    const info = prices.json().prices.nether_star;
    expect(info.confirmed).toBe(true);
    expect(info.lowestUnitCents).toBe(80_000_000);
    expect(info.listings).toBe(1);

    const items = await app.inject({ method: "GET", url: "/v1/items" });
    expect(items.json().items[0].key).toBe("nether_star");
  });

  it("rate limits uploads per client", async () => {
    const { app, time } = server();
    let limited = 0;
    for (let i = 0; i < 35; i++) {
      const response = await app.inject({
        method: "POST",
        url: "/v1/observations",
        payload: batch(CLIENT_A, new Date(time()).toISOString(), 1_000 + i),
      });
      if (response.statusCode === 429) {
        limited++;
      }
    }
    expect(limited).toBe(5);
  });
});
