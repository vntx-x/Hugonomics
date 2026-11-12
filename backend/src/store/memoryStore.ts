import type { SpawnerOffer, SpawnerRating, SpawnerRequest } from "../domain/spawnerOffers.js";
import type { ItemSummary, ListingRecord } from "../domain/types.js";
import type { ClientRecord, SnapshotRecord, Store } from "./store.js";

export class MemoryStore implements Store {
  private readonly listings = new Map<string, ListingRecord>();
  private readonly clients = new Map<string, ClientRecord>();
  private readonly snapshots = new Map<string, SnapshotRecord>();
  private readonly offers = new Map<string, SpawnerOffer>();
  private readonly requests = new Map<string, SpawnerRequest>();
  private readonly ratings = new Map<string, SpawnerRating>();
  readonly observations: { clientId: string; listingId: string; seenAt: number }[] = [];

  async getListings(ids: string[]): Promise<Map<string, ListingRecord>> {
    const result = new Map<string, ListingRecord>();
    for (const id of ids) {
      const record = this.listings.get(id);
      if (record) {
        result.set(id, record);
      }
    }
    return result;
  }

  async putListings(records: ListingRecord[]): Promise<void> {
    for (const record of records) {
      this.listings.set(record.listingId, record);
    }
  }

  async recordObservations(clientId: string, listingIds: string[], seenAt: number): Promise<void> {
    for (const listingId of listingIds) {
      this.observations.push({ clientId, listingId, seenAt });
    }
  }

  async activeListingsByKeys(keys: string[], since: number): Promise<ListingRecord[]> {
    const wanted = new Set(keys);
    return [...this.listings.values()].filter((r) => wanted.has(r.key) && r.lastSeen >= since);
  }

  async activeListings(since: number): Promise<ListingRecord[]> {
    return [...this.listings.values()].filter((r) => r.lastSeen >= since);
  }

  async items(since: number): Promise<ItemSummary[]> {
    const byKey = new Map<string, ItemSummary>();
    for (const record of this.listings.values()) {
      if (record.lastSeen < since) {
        continue;
      }
      const existing = byKey.get(record.key);
      if (existing) {
        existing.activeListings++;
        existing.lastSeen = Math.max(existing.lastSeen, record.lastSeen);
      } else {
        byKey.set(record.key, {
          key: record.key,
          baseId: record.baseId,
          displayName: record.displayName,
          activeListings: 1,
          lastSeen: record.lastSeen,
        });
      }
    }
    return [...byKey.values()].sort((a, b) => b.activeListings - a.activeListings);
  }

  async touchClient(client: ClientRecord): Promise<void> {
    this.clients.set(client.clientId, client);
  }

  async client(clientId: string): Promise<ClientRecord | null> {
    return this.clients.get(clientId) ?? null;
  }

  async putSnapshots(snapshots: SnapshotRecord[]): Promise<void> {
    for (const snapshot of snapshots) {
      this.snapshots.set(`${snapshot.key}|${snapshot.bucketTs}`, snapshot);
    }
  }

  async history(key: string, from: number, to: number): Promise<SnapshotRecord[]> {
    return [...this.snapshots.values()]
      .filter((s) => s.key === key && s.bucketTs >= from && s.bucketTs <= to)
      .sort((a, b) => a.bucketTs - b.bucketTs);
  }

  async purge(observationsBefore: number, listingsLastSeenBefore: number): Promise<{ observations: number; listings: number }> {
    const keptObservations = this.observations.filter((o) => o.seenAt >= observationsBefore);
    const removedObservations = this.observations.length - keptObservations.length;
    this.observations.length = 0;
    this.observations.push(...keptObservations);
    let removedListings = 0;
    for (const [id, record] of this.listings) {
      if (record.lastSeen < listingsLastSeenBefore) {
        this.listings.delete(id);
        removedListings++;
      }
    }
    return { observations: removedObservations, listings: removedListings };
  }

  async spawnerOffers(now: number): Promise<SpawnerOffer[]> {
    return [...this.offers.values()].filter((o) => o.expiresAt > now);
  }

  async putSpawnerOffer(offer: SpawnerOffer): Promise<void> {
    this.offers.set(offer.id, offer);
  }

  async deleteSpawnerOffer(id: string, ownerHash: string): Promise<boolean> {
    const offer = this.offers.get(id);
    if (!offer || offer.ownerHash !== ownerHash) {
      return false;
    }
    return this.offers.delete(id);
  }

  async spawnerRequests(now: number): Promise<SpawnerRequest[]> {
    return [...this.requests.values()].filter((r) => r.expiresAt > now);
  }

  async putSpawnerRequest(request: SpawnerRequest): Promise<void> {
    this.requests.set(request.id, request);
  }

  async deleteSpawnerRequests(ids: string[]): Promise<void> {
    for (const id of ids) {
      this.requests.delete(id);
    }
  }

  async spawnerRatings(): Promise<SpawnerRating[]> {
    return [...this.ratings.values()];
  }

  async putSpawnerRating(rating: SpawnerRating): Promise<boolean> {
    const key = `${rating.requestId}|${rating.fromHash}`;
    if (this.ratings.has(key)) {
      return false;
    }
    this.ratings.set(key, rating);
    return true;
  }
}
