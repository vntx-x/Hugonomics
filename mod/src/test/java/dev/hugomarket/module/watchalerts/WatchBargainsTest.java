package dev.hugomarket.module.watchalerts;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.store.PriceStats;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatchBargainsTest {
    private static ListingIndex.Entry entry(String id, long priceCents) {
        ItemView item = new ItemView("minecraft:" + id, 1, id, List.of(), Map.of(), Map.of(), null, null, List.of(), null, null, Map.of(), List.of());
        Listing listing = new Listing(0, item, new ItemKey(id, "", false), priceCents, "", 3600, ListingStatus.ACTIVE, false);
        return new ListingIndex.Entry("remote:" + id + priceCents, listing, 0, 0, 0);
    }

    private static PriceStats stats(ItemKey key) {
        return new PriceStats(50_00, 100_00, 10, 10, 0);
    }

    @Test
    void firstRunIsSilentThenOnlyNewBargainsFireOnce() {
        WatchBargains bargains = new WatchBargains();
        Set<String> watched = Set.of("diamond");
        assertTrue(bargains.check(List.of(), watched, WatchBargainsTest::stats).isEmpty());
        assertTrue(bargains.check(List.of(entry("diamond", 80_00)), watched, WatchBargainsTest::stats).isEmpty());
        List<ListingIndex.Entry> next = List.of(entry("diamond", 80_00), entry("diamond", 70_00), entry("diamond", 95_00),
                entry("emerald", 10_00));
        List<WatchBargains.Hit> hits = bargains.check(next, watched, WatchBargainsTest::stats);
        assertEquals(1, hits.size());
        assertEquals(70_00, hits.get(0).entry().listing().priceCents());
        assertEquals(0.7, hits.get(0).ratio(), 1e-9);
        assertTrue(bargains.check(next, watched, WatchBargainsTest::stats).isEmpty());
    }
}
