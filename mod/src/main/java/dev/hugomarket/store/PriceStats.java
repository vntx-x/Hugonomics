package dev.hugomarket.store;

import java.util.Arrays;

public record PriceStats(long lowestUnitCents, long medianUnitCents, int listings, int units, long lastSeen) {
    public static PriceStats of(long[] unitPrices, int[] counts, long lastSeen) {
        if (unitPrices.length == 0) {
            return null;
        }
        Integer[] order = new Integer[unitPrices.length];
        int units = 0;
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
            units += Math.max(1, counts[i]);
        }
        Arrays.sort(order, (a, b) -> Long.compare(unitPrices[a], unitPrices[b]));
        long half = (units + 1) / 2;
        long seen = 0;
        long median = unitPrices[order[order.length - 1]];
        for (Integer index : order) {
            seen += Math.max(1, counts[index]);
            if (seen >= half) {
                median = unitPrices[index];
                break;
            }
        }
        return new PriceStats(unitPrices[order[0]], median, unitPrices.length, units, lastSeen);
    }

    public long suggestUndercut(double factor) {
        return Math.max(1, Math.round(lowestUnitCents * factor));
    }
}
