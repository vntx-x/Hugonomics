import { and, desc, eq, gte, inArray, sql } from "drizzle-orm";
import { drizzle, type PostgresJsDatabase } from "drizzle-orm/postgres-js";
import postgres from "postgres";
import type { ItemSummary, ListingRecord } from "../domain/types.js";
import type { ClientRecord, Store } from "../store/store.js";
import { clients, listings, observations } from "./schema.js";

export class PgStore implements Store {
  private readonly db: PostgresJsDatabase;
  private readonly sql: ReturnType<typeof postgres>;

  constructor(databaseUrl: string) {
    this.sql = postgres(databaseUrl, { max: 5 });
    this.db = drizzle(this.sql);
  }

  async close(): Promise<void> {
    await this.sql.end();
  }

  async getListings(ids: string[]): Promise<Map<string, ListingRecord>> {
    if (ids.length === 0) {
      return new Map();
    }
    const rows = await this.db.select().from(listings).where(inArray(listings.listingId, ids));
    return new Map(rows.map((row) => [row.listingId, toRecord(row)]));
  }

  async putListings(records: ListingRecord[]): Promise<void> {
    if (records.length === 0) {
      return;
    }
    await this.db
      .insert(listings)
      .values(records.map(toRow))
      .onConflictDoUpdate({
        target: listings.listingId,
        set: {
          lastSeen: sql`excluded.last_seen`,
          observers: sql`excluded.observers`,
          confirmed: sql`excluded.confirmed`,
        },
      });
  }

  async recordObservations(clientId: string, listingIds: string[], seenAt: number): Promise<void> {
    if (listingIds.length === 0) {
      return;
    }
    await this.db.insert(observations).values(listingIds.map((listingId) => ({ clientId, listingId, seenAt })));
  }

  async activeListingsByKeys(keys: string[], since: number): Promise<ListingRecord[]> {
    if (keys.length === 0) {
      return [];
    }
    const rows = await this.db
      .select()
      .from(listings)
      .where(and(inArray(listings.itemKey, keys), gte(listings.lastSeen, since)));
    return rows.map(toRecord);
  }

  async items(since: number): Promise<ItemSummary[]> {
    const rows = await this.db
      .select({
        key: listings.itemKey,
        baseId: sql<string>`min(${listings.baseId})`,
        displayName: sql<string>`min(${listings.displayName})`,
        activeListings: sql<number>`count(*)::int`,
        lastSeen: sql<number>`max(${listings.lastSeen})::bigint`,
      })
      .from(listings)
      .where(gte(listings.lastSeen, since))
      .groupBy(listings.itemKey)
      .orderBy(desc(sql`count(*)`));
    return rows.map((row) => ({ ...row, lastSeen: Number(row.lastSeen) }));
  }

  async touchClient(client: ClientRecord): Promise<void> {
    await this.db
      .insert(clients)
      .values(client)
      .onConflictDoUpdate({
        target: clients.clientId,
        set: { lastSeen: client.lastSeen, version: client.version, observations: client.observations },
      });
  }

  async client(clientId: string): Promise<ClientRecord | null> {
    const rows = await this.db.select().from(clients).where(eq(clients.clientId, clientId)).limit(1);
    return rows[0] ?? null;
  }
}

function toRecord(row: typeof listings.$inferSelect): ListingRecord {
  return {
    listingId: row.listingId,
    key: row.itemKey,
    baseId: row.baseId,
    displayName: row.displayName,
    count: row.count,
    priceCents: Number(row.priceCents),
    unitPriceCents: Number(row.unitPriceCents),
    sellerHash: row.sellerHash,
    expiresAt: Number(row.expiresAt),
    firstSeen: Number(row.firstSeen),
    lastSeen: Number(row.lastSeen),
    observers: row.observers,
    confirmed: row.confirmed,
  };
}

function toRow(record: ListingRecord): typeof listings.$inferInsert {
  return {
    listingId: record.listingId,
    itemKey: record.key,
    baseId: record.baseId,
    displayName: record.displayName,
    count: record.count,
    priceCents: record.priceCents,
    unitPriceCents: record.unitPriceCents,
    sellerHash: record.sellerHash,
    expiresAt: record.expiresAt,
    firstSeen: record.firstSeen,
    lastSeen: record.lastSeen,
    observers: record.observers,
    confirmed: record.confirmed,
  };
}
