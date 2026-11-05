package dev.hugomarket.store;

import dev.hugomarket.model.ItemKey;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Inflow {
    public record Rate(long observedMs, int items, double itemsPerMinute, long perHour, long perDay, long valuePerHourCents, String topKey) {
        public boolean ready() {
            return observedMs >= MIN_OBSERVED_MS && items > 0;
        }
    }

    private static final long MIN_OBSERVED_MS = 30_000;

    private final Map<String, Integer> added = new LinkedHashMap<>();
    private long openedAt;
    private int total;

    public void open(long now) {
        added.clear();
        openedAt = now;
        total = 0;
    }

    public void added(Map<String, Integer> delta) {
        for (Map.Entry<String, Integer> entry : delta.entrySet()) {
            added.merge(entry.getKey(), entry.getValue(), Integer::sum);
            total += entry.getValue();
        }
    }

    public Rate rate(MarketSession market, long now) {
        long observed = Math.max(0, now - openedAt);
        double minutes = Math.max(observed, 1) / 60_000.0;
        double perMinute = total / minutes;
        long valueCents = 0;
        String topKey = "";
        int topCount = 0;
        for (Map.Entry<String, Integer> entry : added.entrySet()) {
            PriceStats stats = market.stats(ItemKey.parse(entry.getKey()));
            if (stats != null) {
                valueCents += stats.lowestUnitCents() * entry.getValue();
            }
            if (entry.getValue() > topCount) {
                topCount = entry.getValue();
                topKey = entry.getKey();
            }
        }
        long perHour = Math.round(perMinute * 60);
        return new Rate(observed, total, perMinute, perHour, perHour * 24, Math.round(valueCents / minutes * 60), topKey);
    }
}
