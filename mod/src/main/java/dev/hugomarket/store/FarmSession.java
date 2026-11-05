package dev.hugomarket.store;

import dev.hugomarket.model.ItemKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FarmSession {
    public record TopItem(String key, int count, long valueCents) {
    }

    public record Summary(long durationMs, int itemsCollected, long collectedValueCents, long incomeCents,
                          long incomePerHourCents, long collectedPerHourCents, List<TopItem> top) {
    }

    private static final long IDLE_GAP_MS = 10 * 60_000;

    private final Map<String, Integer> collected = new LinkedHashMap<>();
    private long startedAt;
    private long lastActivity;
    private long activeMs;
    private long incomeCents;

    public FarmSession(long now) {
        reset(now);
    }

    public void reset(long now) {
        collected.clear();
        startedAt = now;
        lastActivity = now;
        activeMs = 0;
        incomeCents = 0;
    }

    public void collected(Map<String, Integer> removed, long now) {
        if (removed.isEmpty()) {
            return;
        }
        touch(now);
        for (Map.Entry<String, Integer> entry : removed.entrySet()) {
            collected.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    public void income(long cents, long now) {
        touch(now);
        incomeCents += cents;
    }

    public long startedAt() {
        return startedAt;
    }

    public Summary summary(MarketSession market, long now) {
        long duration = Math.max(activeMs + Math.min(Math.max(0, now - lastActivity), IDLE_GAP_MS), 1);
        int items = 0;
        long value = 0;
        List<TopItem> top = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : collected.entrySet()) {
            items += entry.getValue();
            PriceStats stats = market.stats(ItemKey.parse(entry.getKey()));
            long itemValue = stats == null ? 0 : stats.lowestUnitCents() * entry.getValue();
            value += itemValue;
            top.add(new TopItem(entry.getKey(), entry.getValue(), itemValue));
        }
        top.sort(Comparator.comparingLong(TopItem::valueCents).reversed().thenComparing(Comparator.comparingInt(TopItem::count).reversed()));
        double hours = duration / 3_600_000.0;
        return new Summary(duration, items, value, incomeCents, Math.round(incomeCents / hours), Math.round(value / hours),
                List.copyOf(top.subList(0, Math.min(5, top.size()))));
    }

    private void touch(long now) {
        long gap = now - lastActivity;
        activeMs += Math.min(Math.max(gap, 0), IDLE_GAP_MS);
        lastActivity = now;
    }
}
