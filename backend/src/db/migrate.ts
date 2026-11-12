import postgres from "postgres";
import { loadConfig } from "../config.js";

const statements = [
  `create table if not exists listings (
     listing_id varchar(32) primary key,
     item_key varchar(200) not null,
     base_id varchar(120) not null,
     display_name varchar(120) not null,
     count integer not null,
     price_cents bigint not null,
     unit_price_cents bigint not null,
     seller_hash varchar(64) not null,
     expires_at bigint not null,
     first_seen bigint not null,
     last_seen bigint not null,
     observers jsonb not null,
     confirmed boolean not null
   )`,
  `create index if not exists listings_key_last_seen on listings (item_key, last_seen)`,
  `create table if not exists observations (
     id serial primary key,
     client_id varchar(36) not null,
     listing_id varchar(32) not null,
     seen_at bigint not null
   )`,
  `create index if not exists observations_seen_at on observations (seen_at)`,
  `create table if not exists price_snapshots (
     item_key varchar(200) not null,
     bucket_ts bigint not null,
     lowest_unit_cents bigint not null,
     median_unit_cents bigint not null,
     listings integer not null,
     units integer not null,
     confirmed boolean not null,
     primary key (item_key, bucket_ts)
   )`,
  `create table if not exists clients (
     client_id varchar(36) primary key,
     first_seen bigint not null,
     last_seen bigint not null,
     version text not null,
     observations integer not null
   )`,
  `create table if not exists spawner_offers (
     id varchar(32) primary key,
     type varchar(40) not null,
     side varchar(4) not null,
     count integer not null,
     unit_cents bigint not null,
     player varchar(16) not null,
     owner_hash varchar(64) not null,
     created_at bigint not null,
     expires_at bigint not null
   )`,
  `create index if not exists spawner_offers_expires_at on spawner_offers (expires_at)`,
  `create table if not exists spawner_requests (
     id varchar(32) primary key,
     offer_id varchar(32) not null,
     type varchar(40) not null,
     offer_side varchar(4) not null,
     offer_owner_hash varchar(64) not null,
     offer_owner_player varchar(16) not null,
     from_hash varchar(64) not null,
     from_player varchar(16) not null,
     count integer not null,
     unit_cents bigint not null,
     turn varchar(5) not null,
     status varchar(8) not null,
     rounds integer not null,
     created_at bigint not null,
     updated_at bigint not null,
     expires_at bigint not null
   )`,
  `create index if not exists spawner_requests_expires_at on spawner_requests (expires_at)`,
  `create table if not exists spawner_ratings (
     request_id varchar(32) not null,
     from_hash varchar(64) not null,
     to_hash varchar(64) not null,
     stars integer not null,
     created_at bigint not null,
     primary key (request_id, from_hash)
   )`,
];

async function main(): Promise<void> {
  const config = loadConfig();
  if (!config.databaseUrl) {
    throw new Error("DATABASE_URL is not set");
  }
  const sql = postgres(config.databaseUrl, { max: 1, onnotice: () => {} });
  for (const statement of statements) {
    await sql.unsafe(statement);
  }
  await sql.end();
  console.log(`applied ${statements.length} statements`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
