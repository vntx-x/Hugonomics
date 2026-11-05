import { mergeObservation, type ConsensusRules } from "../domain/consensus.js";
import { candidateListingIds, expiresAtOf } from "../domain/listingId.js";
import type { ListingRecord, ObservationBatch } from "../domain/types.js";
import type { Store } from "../store/store.js";

export interface IngestResult {
  accepted: number;
  newListings: number;
  confirmed: number;
}

export class IngestService {
  constructor(
    private readonly store: Store,
    private readonly rules: ConsensusRules,
  ) {}

  async ingest(batch: ObservationBatch, now: number): Promise<IngestResult> {
    const observedAt = clampObservedAt(Date.parse(batch.observedAt), now);
    const candidates = batch.listings.map((l) =>
      candidateListingIds(l.sellerHash, l.key, l.count, l.priceCents, expiresAtOf(observedAt, l.expiresInSeconds)),
    );
    const existing = await this.store.getListings(candidates.flat());
    const merged = new Map<string, ListingRecord>();
    const seenIds: string[] = [];
    let newListings = 0;
    let confirmed = 0;
    batch.listings.forEach((observation, i) => {
      const ids = candidates[i]!;
      const id = ids.find((candidate) => existing.has(candidate) || merged.has(candidate)) ?? ids[0]!;
      const before = merged.get(id) ?? existing.get(id);
      const record = mergeObservation(before, observation, batch.clientId, observedAt, this.rules);
      if (!before) {
        newListings++;
      }
      if (record.confirmed) {
        confirmed++;
      }
      merged.set(id, { ...record, listingId: id });
      seenIds.push(id);
    });
    await this.store.putListings([...merged.values()]);
    await this.store.recordObservations(batch.clientId, seenIds, observedAt);
    const client = await this.store.client(batch.clientId);
    await this.store.touchClient({
      clientId: batch.clientId,
      firstSeen: client?.firstSeen ?? now,
      lastSeen: now,
      version: batch.clientVersion,
      observations: (client?.observations ?? 0) + batch.listings.length,
    });
    return { accepted: batch.listings.length, newListings, confirmed };
  }
}

function clampObservedAt(observedAt: number, now: number): number {
  if (Number.isNaN(observedAt)) {
    return now;
  }
  const maxSkew = 10 * 60 * 1000;
  return Math.min(Math.max(observedAt, now - maxSkew), now + maxSkew);
}
