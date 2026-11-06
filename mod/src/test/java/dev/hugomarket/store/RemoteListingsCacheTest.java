package dev.hugomarket.store;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.module.ahsearch.ListingIndex;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoteListingsCacheTest {
    private static ListingIndex.RemoteEntry entry(String id, long price, long lastSeen, long expiresAt) {
        ItemView item = new ItemView("minecraft:diamond", 1, "Diamant", List.of(), Map.of(), Map.of(), null, null, List.of(), null, null, Map.of(), List.of());
        Listing listing = new Listing(-1, item, ItemKey.parse("diamond"), price, "", 3600, ListingStatus.ACTIVE, false);
        return new ListingIndex.RemoteEntry(id, listing, lastSeen, expiresAt);
    }

    @Test
    void mergeUpdatesAndAddsWithoutDroppingOthers() {
        RemoteListingsCache cache = new RemoteListingsCache(60_000);
        cache.replace(List.of(entry("a", 100, 1_000, 0), entry("b", 200, 1_000, 0)));
        cache.merge(List.of(entry("b", 150, 2_000, 0), entry("c", 300, 2_000, 0)));
        List<ListingIndex.RemoteEntry> active = cache.active(3_000);
        assertEquals(3, active.size());
        assertEquals(150, active.stream().filter(e -> e.id().equals("b")).findFirst().orElseThrow().listing().priceCents());
    }

    @Test
    void dropsExpiredAndStale() {
        RemoteListingsCache cache = new RemoteListingsCache(60_000);
        cache.replace(List.of(entry("old", 100, 0, 0), entry("gone", 100, 50_000, 55_000), entry("ok", 100, 50_000, 0)));
        List<ListingIndex.RemoteEntry> active = cache.active(70_000);
        assertEquals(1, active.size());
        assertEquals("ok", active.get(0).id());
        assertEquals(1, cache.size());
    }
}
