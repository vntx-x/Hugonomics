package dev.hugomarket.store;

import java.util.Arrays;

public record PriceStats(long lowestUnitCents, long medianUnitCents, int listings, int units, long lastSeen) {
    public static PriceStats of(long[] unitPrices, int[] counts, long lastSeen) {
        if (unitPrices.length == 0) {
            return null;
        }
        long[] sorted = unitPrices.clone();
        Arrays.sort(sorted);
        int units = 0;
        for (int count : counts) {
            units += count;
        }
        int mid = sorted.length / 2;
        long median = sorted.length % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;
        return new PriceStats(sorted[0], median, sorted.length, units, lastSeen);
    }

    public long suggestUndercut(double factor) {
        return Math.max(1, Math.round(lowestUnitCents * factor));
    }
}
