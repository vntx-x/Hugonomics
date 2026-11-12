import { and, asc, desc, eq, gt, gte, inArray, lt, lte, sql } from "drizzle-orm";
import { drizzle, type PostgresJsDatabase } from "drizzle-orm/postgres-js";
import postgres from "postgres";
import type { SpawnerOffer, SpawnerRating, SpawnerRequest } from "../domain/spawnerOffers.js";
import type { ItemSummary, ListingRecord } from "../domain/types.js";
import type { ClientRecord, SnapshotRecord, Store } from "../store/store.js";
import { clients, listings, observations, priceSnapshots, spawnerOffers, spawnerRatings, spawnerRequests } from "./schema.js";

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

  async activeListings(since: number): Promise<ListingRecord[]> {
    const rows = await this.db.select().from(listings).where(gte(listings.lastSeen, since));
    return rows.map(toRecord);
  }

  async putSnapshots(snapshots: SnapshotRecord[]): Promise<void> {
    if (snapshots.length === 0) {
      return;
    }
    await this.db
      .insert(priceSnapshots)
      .values(snapshots.map((s) => ({
        itemKey: s.key,
        bucketTs: s.bucketTs,
        lowestUnitCents: s.lowestUnitCents,
        medianUnitCents: s.medianUnitCents,
        listings: s.listings,
        units: s.units,
        confirmed: s.confirmed,
      })))
      .onConflictDoNothing();
  }

  async history(key: string, from: number, to: number): Promise<SnapshotRecord[]> {
    const rows = await this.db
      .select()
      .from(priceSnapshots)
      .where(and(eq(priceSnapshots.itemKey, key), gte(priceSnapshots.bucketTs, from), lte(priceSnapshots.bucketTs, to)))
      .orderBy(asc(priceSnapshots.bucketTs));
    return rows.map((row) => ({
      key: row.itemKey,
      bucketTs: Number(row.bucketTs),
      lowestUnitCents: Number(row.lowestUnitCents),
      medianUnitCents: Number(row.medianUnitCents),
      listings: row.listings,
      units: row.units,
      confirmed: row.confirmed,
    }));
  }

  async purge(observationsBefore: number, listingsLastSeenBefore: number): Promise<{ observations: number; listings: number }> {
    const removedObservations = await this.db.delete(observations).where(lt(observations.seenAt, observationsBefore)).returning({ id: observations.id });
    const removedListings = await this.db.delete(listings).where(lt(listings.lastSeen, listingsLastSeenBefore)).returning({ id: listings.listingId });
    return { observations: removedObservations.length, listings: removedListings.length };
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

  async spawnerOffers(now: number): Promise<SpawnerOffer[]> {
    const rows = await this.db.select().from(spawnerOffers).where(gt(spawnerOffers.expiresAt, now));
    return rows.map((row) => ({
      ...row,
      side: row.side === "buy" ? "buy" : "sell",
      unitCents: Number(row.unitCents),
      createdAt: Number(row.createdAt),
      expiresAt: Number(row.expiresAt),
    }));
  }

  async putSpawnerOffer(offer: SpawnerOffer): Promise<void> {
    await this.db.insert(spawnerOffers).values(offer);
  }

  async deleteSpawnerOffer(id: string, ownerHash: string): Promise<boolean> {
    const removed = await this.db
      .delete(spawnerOffers)
      .where(and(eq(spawnerOffers.id, id), eq(spawnerOffers.ownerHash, ownerHash)))
      .returning({ id: spawnerOffers.id });
    return removed.length > 0;
  }

  async spawnerRequests(now: number): Promise<SpawnerRequest[]> {
    const rows = await this.db.select().from(spawnerRequests).where(gt(spawnerRequests.expiresAt, now));
    return rows.map((row) => ({
      ...row,
      offerSide: row.offerSide === "buy" ? "buy" : "sell",
      turn: row.turn === "owner" ? "owner" : "from",
      status: row.status === "accepted" || row.status === "declined" ? row.status : "open",
      unitCents: Number(row.unitCents),
      createdAt: Number(row.createdAt),
      updatedAt: Number(row.updatedAt),
      expiresAt: Number(row.expiresAt),
    }));
  }

  async putSpawnerRequest(request: SpawnerRequest): Promise<void> {
    await this.db
      .insert(spawnerRequests)
      .values(request)
      .onConflictDoUpdate({
        target: spawnerRequests.id,
        set: {
          count: request.count,
          unitCents: request.unitCents,
          turn: request.turn,
          status: request.status,
          rounds: request.rounds,
          updatedAt: request.updatedAt,
          expiresAt: request.expiresAt,
        },
      });
  }

  async spawnerRatings(): Promise<SpawnerRating[]> {
    const rows = await this.db.select().from(spawnerRatings);
    return rows.map((row) => ({ ...row, createdAt: Number(row.createdAt) }));
  }

  async putSpawnerRating(rating: SpawnerRating): Promise<boolean> {
    const inserted = await this.db.insert(spawnerRatings).values(rating).onConflictDoNothing().returning({ id: spawnerRatings.requestId });
    return inserted.length > 0;
  }

  async deleteSpawnerRequests(ids: string[]): Promise<void> {
    if (ids.length === 0) {
      return;
    }
    await this.db.delete(spawnerRequests).where(inArray(spawnerRequests.id, ids));
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
