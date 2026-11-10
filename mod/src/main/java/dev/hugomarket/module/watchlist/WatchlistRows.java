package dev.hugomarket.module.watchlist;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.module.ahsearch.Deals;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.store.PriceStats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Function;

public final class WatchlistRows {
    public record Row(String key, ListingIndex.Entry cheapest, int offers, PriceStats stats, OptionalDouble ratio) {
        public boolean hasOffers() {
            return cheapest != null;
        }
    }

    private static final Comparator<Row> ORDER = Comparator
            .comparing((Row row) -> !row.hasOffers())
            .thenComparing(row -> row.ratio().isEmpty())
            .thenComparingDouble(row -> row.ratio().orElse(0));

    private WatchlistRows() {
    }

    public static List<Row> build(Collection<String> keys, List<ListingIndex.Entry> entries, Function<ItemKey, PriceStats> stats) {
        Map<String, ListingIndex.Entry> cheapest = new HashMap<>();
        Map<String, Integer> offers = new HashMap<>();
        for (ListingIndex.Entry entry : entries) {
            String key = entry.listing().key().toString();
            if (!keys.contains(key)) {
                continue;
            }
            offers.merge(key, 1, Integer::sum);
            ListingIndex.Entry best = cheapest.get(key);
            if (best == null || entry.listing().unitPriceCents() < best.listing().unitPriceCents()) {
                cheapest.put(key, entry);
            }
        }
        List<Row> rows = new ArrayList<>(keys.size());
        for (String key : keys) {
            ListingIndex.Entry best = cheapest.get(key);
            PriceStats keyStats = stats.apply(ItemKey.parse(key));
            OptionalDouble ratio = best == null ? OptionalDouble.empty() : Deals.ratio(keyStats, best.listing());
            rows.add(new Row(key, best, offers.getOrDefault(key, 0), keyStats, ratio));
        }
        rows.sort(ORDER);
        return rows;
    }
}
