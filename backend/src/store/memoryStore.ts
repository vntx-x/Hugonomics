import type { ItemSummary, ListingRecord } from "../domain/types.js";
import type { ClientRecord, Store } from "./store.js";

export class MemoryStore implements Store {
  private readonly listings = new Map<string, ListingRecord>();
  private readonly clients = new Map<string, ClientRecord>();
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
}
