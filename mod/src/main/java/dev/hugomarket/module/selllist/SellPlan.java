package dev.hugomarket.module.selllist;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.module.sellassist.SellSuggestion;
import dev.hugomarket.store.Fees;
import dev.hugomarket.store.PriceStats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class SellPlan {
    public record Line(ItemKey key, ItemView sample, int count, long unitCents, long lowestUnitCents, int listings,
                       long totalCents, long netCents) {
    }

    private SellPlan() {
    }

    public static List<Line> build(List<ItemView> items, Function<ItemView, ItemKey> keyOf, Function<ItemKey, PriceStats> stats, Fees fees) {
        Map<String, ItemView> samples = new LinkedHashMap<>();
        Map<String, ItemKey> keys = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemView item : items) {
            ItemKey key = keyOf.apply(item);
            if (key.filled()) {
                continue;
            }
            String id = key.toString();
            samples.putIfAbsent(id, item);
            keys.putIfAbsent(id, key);
            counts.merge(id, Math.max(1, item.count()), Integer::sum);
        }
        List<Line> lines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            ItemKey key = keys.get(entry.getKey());
            PriceStats priceStats = stats.apply(key);
            if (priceStats == null || priceStats.lowestUnitCents() <= 0) {
                continue;
            }
            int count = entry.getValue();
            long unit = priceStats.suggestUndercut(SellSuggestion.UNDERCUT);
            long total = unit * count;
            long net = total - fees.listingFee(total) - fees.saleTax(total);
            lines.add(new Line(key, samples.get(entry.getKey()), count, unit, priceStats.lowestUnitCents(), priceStats.listings(), total, net));
        }
        lines.sort(Comparator.comparingLong(Line::totalCents).reversed());
        return lines;
    }

    public static long totalNet(List<Line> lines) {
        long sum = 0;
        for (Line line : lines) {
            sum += line.netCents();
        }
        return sum;
    }
}
