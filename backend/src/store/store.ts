import type { SpawnerOffer, SpawnerRating, SpawnerRequest } from "../domain/spawnerOffers.js";
import type { ItemSummary, ListingRecord } from "../domain/types.js";

export interface ClientRecord {
  clientId: string;
  firstSeen: number;
  lastSeen: number;
  version: string;
  observations: number;
}

export interface SnapshotRecord {
  key: string;
  bucketTs: number;
  lowestUnitCents: number;
  medianUnitCents: number;
  listings: number;
  units: number;
  confirmed: boolean;
}

export interface Store {
  getListings(ids: string[]): Promise<Map<string, ListingRecord>>;
  putListings(records: ListingRecord[]): Promise<void>;
  recordObservations(clientId: string, listingIds: string[], seenAt: number): Promise<void>;
  activeListingsByKeys(keys: string[], since: number): Promise<ListingRecord[]>;
  activeListings(since: number): Promise<ListingRecord[]>;
  items(since: number): Promise<ItemSummary[]>;
  touchClient(client: ClientRecord): Promise<void>;
  client(clientId: string): Promise<ClientRecord | null>;
  putSnapshots(snapshots: SnapshotRecord[]): Promise<void>;
  history(key: string, from: number, to: number): Promise<SnapshotRecord[]>;
  purge(observationsBefore: number, listingsLastSeenBefore: number): Promise<{ observations: number; listings: number }>;
  spawnerOffers(now: number): Promise<SpawnerOffer[]>;
  putSpawnerOffer(offer: SpawnerOffer): Promise<void>;
  deleteSpawnerOffer(id: string, ownerHash: string): Promise<boolean>;
  spawnerRequests(now: number): Promise<SpawnerRequest[]>;
  putSpawnerRequest(request: SpawnerRequest): Promise<void>;
  deleteSpawnerRequests(ids: string[]): Promise<void>;
  spawnerRatings(): Promise<SpawnerRating[]>;
  putSpawnerRating(rating: SpawnerRating): Promise<boolean>;
}
