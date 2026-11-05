import { createHash } from "node:crypto";

const EXPIRY_BUCKET_MS = 10_000;
const NEIGHBOUR_OFFSETS = [0, -1, 1];

export function expiresAtOf(observedAtMs: number, expiresInSeconds: number): number {
  return expiresInSeconds < 0 ? 0 : observedAtMs + expiresInSeconds * 1000;
}

export function listingIdOf(
  sellerHash: string,
  key: string,
  count: number,
  priceCents: number,
  expiresAt: number,
  bucketOffset = 0,
): string {
  const bucket = Math.round(expiresAt / EXPIRY_BUCKET_MS) + bucketOffset;
  return createHash("sha256")
    .update(`${sellerHash}|${key}|${count}|${priceCents}|${bucket}`)
    .digest("hex")
    .slice(0, 32);
}

export function candidateListingIds(
  sellerHash: string,
  key: string,
  count: number,
  priceCents: number,
  expiresAt: number,
): string[] {
  if (expiresAt === 0) {
    return [listingIdOf(sellerHash, key, count, priceCents, 0)];
  }
  return NEIGHBOUR_OFFSETS.map((offset) => listingIdOf(sellerHash, key, count, priceCents, expiresAt, offset));
}
