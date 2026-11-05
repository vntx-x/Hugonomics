import type { ListingObservation, ListingRecord, PriceInfo } from "./types.js";
import { expiresAtOf, listingIdOf } from "./listingId.js";

const MAX_OBSERVERS = 16;
const OUTLIER_FACTOR = 10;

export interface ConsensusRules {
  confirmGapMs: number;
  activeWindowMs: number;
  trustedClients?: ReadonlySet<string>;
}

function trusted(rules: ConsensusRules, clientId: string): boolean {
  return rules.trustedClients?.has(clientId) ?? false;
}

export function mergeObservation(
  existing: ListingRecord | undefined,
  observation: ListingObservation,
  clientId: string,
  observedAt: number,
  rules: ConsensusRules,
): ListingRecord {
  const expiresAt = expiresAtOf(observedAt, observation.expiresInSeconds);
  const unitPriceCents = Math.round(observation.priceCents / observation.count);
  if (!existing) {
    return {
      listingId: listingIdOf(observation.sellerHash, observation.key, observation.count, observation.priceCents, expiresAt),
      key: observation.key,
      baseId: observation.baseId,
      displayName: observation.displayName,
      count: observation.count,
      priceCents: observation.priceCents,
      unitPriceCents,
      sellerHash: observation.sellerHash,
      expiresAt,
      firstSeen: observedAt,
      lastSeen: observedAt,
      observers: [clientId],
      confirmed: trusted(rules, clientId),
    };
  }
  const observers = existing.observers.includes(clientId)
    ? existing.observers
    : [...existing.observers, clientId].slice(0, MAX_OBSERVERS);
  const lastSeen = Math.max(existing.lastSeen, observedAt);
  const confirmed = existing.confirmed || trusted(rules, clientId) || observers.length >= 2
    || lastSeen - existing.firstSeen >= rules.confirmGapMs;
  return { ...existing, lastSeen, observers, confirmed };
}

export function isActive(record: ListingRecord, now: number, rules: ConsensusRules): boolean {
  if (record.expiresAt > 0 && record.expiresAt <= now) {
    return false;
  }
  return now - record.lastSeen <= rules.activeWindowMs;
}

export function priceInfo(key: string, records: ListingRecord[], now: number, rules: ConsensusRules): PriceInfo | null {
  const active = records.filter((r) => r.key === key && isActive(r, now, rules));
  if (active.length === 0) {
    return null;
  }
  const confirmedOnly = active.filter((r) => r.confirmed);
  const pool = confirmedOnly.length > 0 ? confirmedOnly : active;
  const sorted = [...pool].sort((a, b) => a.unitPriceCents - b.unitPriceCents);
  const rawMedian = medianOf(sorted.map((r) => r.unitPriceCents));
  const trimmed = sorted.filter(
    (r) => r.unitPriceCents * OUTLIER_FACTOR >= rawMedian && r.unitPriceCents <= rawMedian * OUTLIER_FACTOR,
  );
  const used = trimmed.length > 0 ? trimmed : sorted;
  return {
    key,
    lowestUnitCents: used[0]!.unitPriceCents,
    medianUnitCents: medianOf(used.map((r) => r.unitPriceCents)),
    listings: used.length,
    units: used.reduce((sum, r) => sum + r.count, 0),
    lastSeen: Math.max(...used.map((r) => r.lastSeen)),
    confirmed: confirmedOnly.length > 0,
  };
}

function medianOf(sortedValues: number[]): number {
  const mid = Math.floor(sortedValues.length / 2);
  if (sortedValues.length % 2 === 1) {
    return sortedValues[mid]!;
  }
  return Math.round((sortedValues[mid - 1]! + sortedValues[mid]!) / 2);
}
