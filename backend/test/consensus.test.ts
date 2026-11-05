import { describe, expect, it } from "vitest";
import { isActive, mergeObservation, priceInfo } from "../src/domain/consensus.js";
import { candidateListingIds, listingIdOf } from "../src/domain/listingId.js";
import type { ListingObservation, ListingRecord } from "../src/domain/types.js";

const rules = { confirmGapMs: 60_000, activeWindowMs: 6 * 3600_000 };

function observation(overrides: Partial<ListingObservation> = {}): ListingObservation {
  return {
    key: "nether_star",
    baseId: "minecraft:nether_star",
    displayName: "Nether Star",
    count: 1,
    priceCents: 80_000_000,
    sellerHash: "a".repeat(32),
    expiresInSeconds: 30_000,
    slot: 0,
    ...overrides,
  };
}

describe("listing identity", () => {
  it("is stable within a 10 second expiry bucket", () => {
    const a = listingIdOf("s", "k", 1, 100, 1_000_000);
    const b = listingIdOf("s", "k", 1, 100, 1_004_000);
    const c = listingIdOf("s", "k", 1, 100, 1_020_000);
    expect(a).toBe(b);
    expect(a).not.toBe(c);
  });

  it("offers the neighbouring buckets as candidates", () => {
    const candidates = candidateListingIds("s", "k", 1, 100, 1_004_000);
    expect(candidates).toHaveLength(3);
    expect(candidates[0]).toBe(listingIdOf("s", "k", 1, 100, 1_004_000));
    expect(candidates).toContain(listingIdOf("s", "k", 1, 100, 1_012_000));
    expect(candidates).toContain(listingIdOf("s", "k", 1, 100, 992_000));
    expect(candidateListingIds("s", "k", 1, 100, 0)).toHaveLength(1);
  });
});

describe("consensus", () => {
  it("confirms after a second client", () => {
    const first = mergeObservation(undefined, observation(), "c1", 1_000, rules);
    expect(first.confirmed).toBe(false);
    const second = mergeObservation(first, observation(), "c2", 2_000, rules);
    expect(second.confirmed).toBe(true);
    expect(second.observers).toEqual(["c1", "c2"]);
  });

  it("confirms after the same client sees it again a minute later", () => {
    const trustedRules = { ...rules, trustedClients: new Set(["owner"]) };
    const byOwner = mergeObservation(undefined, observation(), "owner", 1_000, trustedRules);
    expect(byOwner.confirmed).toBe(true);
    const byOther = mergeObservation(undefined, observation(), "c9", 1_000, trustedRules);
    expect(byOther.confirmed).toBe(false);
    expect(mergeObservation(byOther, observation(), "owner", 2_000, trustedRules).confirmed).toBe(true);
    const first = mergeObservation(undefined, observation(), "c1", 1_000, rules);
    const again = mergeObservation(first, observation(), "c1", 30_000, rules);
    expect(again.confirmed).toBe(false);
    const later = mergeObservation(again, observation(), "c1", 61_000, rules);
    expect(later.confirmed).toBe(true);
  });

  it("drops expired and stale listings from the active set", () => {
    const record = mergeObservation(undefined, observation({ expiresInSeconds: 10 }), "c1", 1_000, rules);
    expect(isActive(record, 5_000, rules)).toBe(true);
    expect(isActive(record, 12_000, rules)).toBe(false);
    const stale = mergeObservation(undefined, observation(), "c1", 1_000, rules);
    expect(isActive(stale, 1_000 + rules.activeWindowMs + 1, rules)).toBe(false);
  });
});

describe("price info", () => {
  function records(prices: number[], confirmed = true): ListingRecord[] {
    return prices.map((price, i) => ({
      ...mergeObservation(undefined, observation({ priceCents: price, sellerHash: `${i}`.padStart(32, "0") }), "c1", 1_000, rules),
      confirmed,
    }));
  }

  it("prefers confirmed listings and trims outliers", () => {
    const info = priceInfo("nether_star", records([100_000, 110_000, 120_000, 5_000_000, 50]), 2_000, rules)!;
    expect(info.lowestUnitCents).toBe(100_000);
    expect(info.medianUnitCents).toBe(110_000);
    expect(info.listings).toBe(3);
    expect(info.confirmed).toBe(true);
  });

  it("falls back to unconfirmed listings when nothing is confirmed", () => {
    const info = priceInfo("nether_star", records([300, 100, 200], false), 2_000, rules)!;
    expect(info.lowestUnitCents).toBe(100);
    expect(info.medianUnitCents).toBe(200);
    expect(info.confirmed).toBe(false);
  });

  it("returns null without listings", () => {
    expect(priceInfo("dragon_egg", [], 2_000, rules)).toBeNull();
  });
});
