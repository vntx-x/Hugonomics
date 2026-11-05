import { priceInfo, type ConsensusRules } from "../domain/consensus.js";
import type { SnapshotRecord, Store } from "../store/store.js";

export const SNAPSHOT_BUCKET_MS = 5 * 60 * 1000;

export class SnapshotJob {
  constructor(
    private readonly store: Store,
    private readonly rules: ConsensusRules,
  ) {}

  async run(now: number): Promise<number> {
    const records = await this.store.activeListings(now - this.rules.activeWindowMs);
    const keys = [...new Set(records.map((r) => r.key))];
    const bucketTs = Math.floor(now / SNAPSHOT_BUCKET_MS) * SNAPSHOT_BUCKET_MS;
    const snapshots: SnapshotRecord[] = [];
    for (const key of keys) {
      const info = priceInfo(key, records, now, this.rules);
      if (!info) {
        continue;
      }
      snapshots.push({
        key,
        bucketTs,
        lowestUnitCents: info.lowestUnitCents,
        medianUnitCents: info.medianUnitCents,
        listings: info.listings,
        units: info.units,
        confirmed: info.confirmed,
      });
    }
    await this.store.putSnapshots(snapshots);
    return snapshots.length;
  }
}

export class RetentionJob {
  constructor(
    private readonly store: Store,
    private readonly observationDays: number,
    private readonly listingDays: number,
  ) {}

  async run(now: number): Promise<{ observations: number; listings: number }> {
    const day = 24 * 60 * 60 * 1000;
    return this.store.purge(now - this.observationDays * day, now - this.listingDays * day);
  }
}

export function startJobs(
  store: Store,
  rules: ConsensusRules,
  log: (message: string) => void,
): () => void {
  const snapshots = new SnapshotJob(store, rules);
  const retention = new RetentionJob(store, 30, 90);
  const snapshotTimer = setInterval(() => {
    snapshots.run(Date.now()).then((count) => log(`snapshot: ${count} keys`)).catch((error) => log(`snapshot failed: ${error}`));
  }, SNAPSHOT_BUCKET_MS);
  const retentionTimer = setInterval(() => {
    retention.run(Date.now()).then((r) => log(`retention: ${r.observations} observations, ${r.listings} listings removed`)).catch((error) => log(`retention failed: ${error}`));
  }, 60 * 60 * 1000);
  return () => {
    clearInterval(snapshotTimer);
    clearInterval(retentionTimer);
  };
}
