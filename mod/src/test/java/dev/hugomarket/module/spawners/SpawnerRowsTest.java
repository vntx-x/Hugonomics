package dev.hugomarket.module.spawners;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.module.watchlist.WatchlistRows;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpawnerRowsTest {
    private static ListingIndex.Entry entry(String id, String key, int count, long priceCents) {
        ItemView item = new ItemView(id, count, key, List.of(), Map.of(), Map.of(), null, null, List.of(), null, null, Map.of(), List.of());
        Listing listing = new Listing(0, item, new ItemKey(key, "", false), priceCents, "", 3600, ListingStatus.ACTIVE, false);
        return new ListingIndex.Entry("remote:" + key + count + priceCents, listing, 0, 0, 0);
    }

    @Test
    void listsOnlySpawnersByPricePerSpawnerHighestFirst() {
        List<ListingIndex.Entry> entries = List.of(
                entry("minecraft:spawner", "hugosmp:skeleton_spawner", 8, 4_000_000_00),
                entry("minecraft:spawner", "hugosmp:skeleton_spawner", 1, 600_000_00),
                entry("minecraft:spawner", "hugosmp:iron_golem_spawner", 2, 3_000_000_00),
                entry("minecraft:diamond", "diamond", 64, 6_400_00));
        List<WatchlistRows.Row> rows = SpawnerRows.build(entries, key -> null);
        assertEquals(List.of("hugosmp:iron_golem_spawner", "hugosmp:skeleton_spawner"), rows.stream().map(WatchlistRows.Row::key).toList());
        assertEquals(500_000_00, rows.get(1).cheapest().listing().unitPriceCents());
        assertEquals(2, rows.get(1).offers());
    }
}
