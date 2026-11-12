package dev.hugomarket.module.spawners;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.module.watchlist.WatchlistRows;
import dev.hugomarket.store.PriceStats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

final class SpawnerRows {
    private static final String SPAWNER_ID = "minecraft:spawner";
    private static final Comparator<WatchlistRows.Row> ORDER = Comparator
            .comparingLong((WatchlistRows.Row row) -> row.cheapest().listing().unitPriceCents()).reversed();

    private SpawnerRows() {
    }

    static List<WatchlistRows.Row> build(List<ListingIndex.Entry> entries, Function<ItemKey, PriceStats> stats) {
        Set<String> keys = new LinkedHashSet<>();
        for (ListingIndex.Entry entry : entries) {
            if (SPAWNER_ID.equals(entry.listing().item().id())) {
                keys.add(entry.listing().key().toString());
            }
        }
        List<WatchlistRows.Row> rows = new ArrayList<>(WatchlistRows.build(keys, entries, stats));
        rows.sort(ORDER);
        return rows;
    }
}
