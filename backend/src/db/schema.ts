import { bigint, boolean, index, integer, jsonb, pgTable, primaryKey, serial, text, varchar } from "drizzle-orm/pg-core";

export const listings = pgTable(
  "listings",
  {
    listingId: varchar("listing_id", { length: 32 }).primaryKey(),
    itemKey: varchar("item_key", { length: 200 }).notNull(),
    baseId: varchar("base_id", { length: 120 }).notNull(),
    displayName: varchar("display_name", { length: 120 }).notNull(),
    count: integer("count").notNull(),
    priceCents: bigint("price_cents", { mode: "number" }).notNull(),
    unitPriceCents: bigint("unit_price_cents", { mode: "number" }).notNull(),
    sellerHash: varchar("seller_hash", { length: 64 }).notNull(),
    expiresAt: bigint("expires_at", { mode: "number" }).notNull(),
    firstSeen: bigint("first_seen", { mode: "number" }).notNull(),
    lastSeen: bigint("last_seen", { mode: "number" }).notNull(),
    observers: jsonb("observers").$type<string[]>().notNull(),
    confirmed: boolean("confirmed").notNull(),
  },
  (table) => [index("listings_key_last_seen").on(table.itemKey, table.lastSeen)],
);

export const observations = pgTable(
  "observations",
  {
    id: serial("id").primaryKey(),
    clientId: varchar("client_id", { length: 36 }).notNull(),
    listingId: varchar("listing_id", { length: 32 }).notNull(),
    seenAt: bigint("seen_at", { mode: "number" }).notNull(),
  },
  (table) => [index("observations_seen_at").on(table.seenAt)],
);

export const priceSnapshots = pgTable(
  "price_snapshots",
  {
    itemKey: varchar("item_key", { length: 200 }).notNull(),
    bucketTs: bigint("bucket_ts", { mode: "number" }).notNull(),
    lowestUnitCents: bigint("lowest_unit_cents", { mode: "number" }).notNull(),
    medianUnitCents: bigint("median_unit_cents", { mode: "number" }).notNull(),
    listings: integer("listings").notNull(),
    units: integer("units").notNull(),
    confirmed: boolean("confirmed").notNull(),
  },
  (table) => [primaryKey({ columns: [table.itemKey, table.bucketTs] })],
);

export const spawnerOffers = pgTable(
  "spawner_offers",
  {
    id: varchar("id", { length: 32 }).primaryKey(),
    type: varchar("type", { length: 40 }).notNull(),
    side: varchar("side", { length: 4 }).notNull(),
    count: integer("count").notNull(),
    unitCents: bigint("unit_cents", { mode: "number" }).notNull(),
    player: varchar("player", { length: 16 }).notNull(),
    ownerHash: varchar("owner_hash", { length: 64 }).notNull(),
    createdAt: bigint("created_at", { mode: "number" }).notNull(),
    expiresAt: bigint("expires_at", { mode: "number" }).notNull(),
  },
  (table) => [index("spawner_offers_expires_at").on(table.expiresAt)],
);

export const spawnerRequests = pgTable(
  "spawner_requests",
  {
    id: varchar("id", { length: 32 }).primaryKey(),
    offerId: varchar("offer_id", { length: 32 }).notNull(),
    type: varchar("type", { length: 40 }).notNull(),
    offerSide: varchar("offer_side", { length: 4 }).notNull(),
    offerOwnerHash: varchar("offer_owner_hash", { length: 64 }).notNull(),
    offerOwnerPlayer: varchar("offer_owner_player", { length: 16 }).notNull(),
    fromHash: varchar("from_hash", { length: 64 }).notNull(),
    fromPlayer: varchar("from_player", { length: 16 }).notNull(),
    count: integer("count").notNull(),
    unitCents: bigint("unit_cents", { mode: "number" }).notNull(),
    turn: varchar("turn", { length: 5 }).notNull(),
    status: varchar("status", { length: 8 }).notNull(),
    rounds: integer("rounds").notNull(),
    createdAt: bigint("created_at", { mode: "number" }).notNull(),
    updatedAt: bigint("updated_at", { mode: "number" }).notNull(),
    expiresAt: bigint("expires_at", { mode: "number" }).notNull(),
  },
  (table) => [index("spawner_requests_expires_at").on(table.expiresAt)],
);

export const spawnerRatings = pgTable(
  "spawner_ratings",
  {
    requestId: varchar("request_id", { length: 32 }).notNull(),
    fromHash: varchar("from_hash", { length: 64 }).notNull(),
    toHash: varchar("to_hash", { length: 64 }).notNull(),
    stars: integer("stars").notNull(),
    createdAt: bigint("created_at", { mode: "number" }).notNull(),
  },
  (table) => [primaryKey({ columns: [table.requestId, table.fromHash] })],
);

export const clients = pgTable("clients", {
  clientId: varchar("client_id", { length: 36 }).primaryKey(),
  firstSeen: bigint("first_seen", { mode: "number" }).notNull(),
  lastSeen: bigint("last_seen", { mode: "number" }).notNull(),
  version: text("version").notNull(),
  observations: integer("observations").notNull(),
});
