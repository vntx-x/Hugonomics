import type { ItemSummary, ListingRecord } from "../domain/types.js";

export interface ClientRecord {
  clientId: string;
  firstSeen: number;
  lastSeen: number;
  version: string;
  observations: number;
}

export interface Store {
  getListings(ids: string[]): Promise<Map<string, ListingRecord>>;
  putListings(records: ListingRecord[]): Promise<void>;
  recordObservations(clientId: string, listingIds: string[], seenAt: number): Promise<void>;
  activeListingsByKeys(keys: string[], since: number): Promise<ListingRecord[]>;
  items(since: number): Promise<ItemSummary[]>;
  touchClient(client: ClientRecord): Promise<void>;
  client(clientId: string): Promise<ClientRecord | null>;
}
