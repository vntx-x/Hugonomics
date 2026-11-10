package dev.hugomarket.module.watchlist;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.store.PriceStats;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatchlistRowsTest {
    private static ListingIndex.Entry entry(String id, int count, long priceCents) {
        ItemView item = new ItemView("minecraft:" + id, count, id, List.of(), Map.of(), Map.of(), null, null, List.of(), null, null, Map.of(), List.of());
        Listing listing = new Listing(0, item, new ItemKey(id, "", false), priceCents, "", 3600, ListingStatus.ACTIVE, false);
        return new ListingIndex.Entry("remote:" + id + priceCents, listing, 0, 0, 0);
    }

    private static PriceStats stats(ItemKey key) {
        return switch (key.base()) {
            case "diamond" -> new PriceStats(90_00, 100_00, 10, 10, 0);
            case "emerald" -> new PriceStats(40_00, 50_00, 10, 10, 0);
            default -> null;
        };
    }

    @Test
    void picksCheapestPerUnitAndCountsOffers() {
        List<ListingIndex.Entry> entries = List.of(entry("diamond", 2, 220_00), entry("diamond", 1, 95_00),
                entry("diamond", 4, 400_00), entry("gold_ingot", 1, 5_00));
        List<WatchlistRows.Row> rows = WatchlistRows.build(List.of("diamond"), entries, WatchlistRowsTest::stats);
        assertEquals(1, rows.size());
        assertEquals(3, rows.get(0).offers());
        assertEquals(95_00, rows.get(0).cheapest().listing().priceCents());
        assertEquals(0.95, rows.get(0).ratio().getAsDouble(), 1e-9);
    }

    @Test
    void sortsCheapestRelativeFirstAndEmptyLast() {
        LinkedHashSet<String> keys = new LinkedHashSet<>(List.of("nether_star", "diamond", "beacon", "emerald"));
        List<ListingIndex.Entry> entries = List.of(entry("diamond", 1, 110_00), entry("emerald", 1, 30_00), entry("beacon", 1, 1_000_00));
        List<WatchlistRows.Row> rows = WatchlistRows.build(keys, entries, WatchlistRowsTest::stats);
        assertEquals(List.of("emerald", "diamond", "beacon", "nether_star"), rows.stream().map(WatchlistRows.Row::key).toList());
        assertTrue(rows.get(2).hasOffers());
        assertTrue(rows.get(2).ratio().isEmpty());
        assertFalse(rows.get(3).hasOffers());
        assertEquals(0, rows.get(3).offers());
    }
}
