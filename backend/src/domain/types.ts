export interface ListingObservation {
  key: string;
  baseId: string;
  displayName: string;
  count: number;
  priceCents: number;
  sellerHash: string;
  expiresInSeconds: number;
  slot: number;
}

export interface ObservationBatch {
  clientId: string;
  clientVersion: string;
  mcVersion: string;
  observedAt: string;
  source: "ah_list";
  listings: ListingObservation[];
}

export interface ListingRecord {
  listingId: string;
  key: string;
  baseId: string;
  displayName: string;
  count: number;
  priceCents: number;
  unitPriceCents: number;
  sellerHash: string;
  expiresAt: number;
  firstSeen: number;
  lastSeen: number;
  observers: string[];
  confirmed: boolean;
}

export interface PriceInfo {
  key: string;
  lowestUnitCents: number;
  medianUnitCents: number;
  listings: number;
  units: number;
  lastSeen: number;
  confirmed: boolean;
}

export interface ItemSummary {
  key: string;
  baseId: string;
  displayName: string;
  activeListings: number;
  lastSeen: number;
}
