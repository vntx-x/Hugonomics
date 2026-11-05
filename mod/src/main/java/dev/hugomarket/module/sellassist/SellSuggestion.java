package dev.hugomarket.module.sellassist;

import dev.hugomarket.store.PriceStats;

import java.util.OptionalLong;

public final class SellSuggestion {
    public static final double UNDERCUT = 0.95;
    private static final int MIN_LISTINGS_FOR_WARNING = 3;
    private static final double TYPO_FACTOR = 10.0;

    public enum Warning {
        NONE,
        FAR_BELOW,
        FAR_ABOVE
    }

    private SellSuggestion() {
    }

    public static long undercutStackDollars(PriceStats stats, int count) {
        return toDollars(stats.suggestUndercut(UNDERCUT) * count);
    }

    public static long lowestStackDollars(PriceStats stats, int count) {
        return toDollars(stats.lowestUnitCents() * count);
    }

    public static long medianStackDollars(PriceStats stats, int count) {
        return toDollars(stats.medianUnitCents() * count);
    }

    public static OptionalLong parseTypedDollars(String text) {
        String digits = text.replace(".", "").replace(",", "").trim();
        if (digits.isEmpty() || !digits.chars().allMatch(Character::isDigit) || digits.length() > 15) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Long.parseLong(digits));
    }

    public static Warning check(PriceStats stats, int count, long typedDollars) {
        if (stats == null || stats.listings() < MIN_LISTINGS_FOR_WARNING || typedDollars <= 0) {
            return Warning.NONE;
        }
        double marketStack = stats.medianUnitCents() * count / 100.0;
        if (typedDollars * TYPO_FACTOR < marketStack) {
            return Warning.FAR_BELOW;
        }
        if (typedDollars > marketStack * TYPO_FACTOR) {
            return Warning.FAR_ABOVE;
        }
        return Warning.NONE;
    }

    private static long toDollars(long cents) {
        return Math.max(1, Math.floorDiv(cents, 100));
    }
}
