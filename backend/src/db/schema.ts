import { bigint, boolean, index, integer, jsonb, pgTable, serial, text, varchar } from "drizzle-orm/pg-core";

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

export const clients = pgTable("clients", {
  clientId: varchar("client_id", { length: 36 }).primaryKey(),
  firstSeen: bigint("first_seen", { mode: "number" }).notNull(),
  lastSeen: bigint("last_seen", { mode: "number" }).notNull(),
  version: text("version").notNull(),
  observations: integer("observations").notNull(),
});
