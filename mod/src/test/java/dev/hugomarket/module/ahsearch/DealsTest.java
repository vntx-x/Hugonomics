package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.parse.ItemKeyFactory;
import dev.hugomarket.store.PriceStats;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DealsTest {
    private static Listing listing(long unitCents) {
        ItemView item = new ItemView("minecraft:diamond", 1, "Diamant", List.of(), Map.of(), Map.of(), null, null, List.of(), null, null, Map.of(), List.of());
        return new Listing(0, item, ItemKeyFactory.of(item), unitCents, "seller", 3600, ListingStatus.ACTIVE, false);
    }

    private static PriceStats stats(long medianCents, int listings) {
        return new PriceStats(medianCents, medianCents, listings, listings, 0);
    }

    @Test
    void ratioNeedsEnoughListings() {
        assertTrue(Deals.ratio(stats(100_00, 2), listing(50_00)).isEmpty());
        assertTrue(Deals.ratio(null, listing(50_00)).isEmpty());
        assertEquals(0.5, Deals.ratio(stats(100_00, 5), listing(50_00)).getAsDouble(), 1e-9);
    }

    @Test
    void bargainBelowEightyFivePercent() {
        assertTrue(Deals.isBargain(stats(100_00, 5), listing(85_00)));
        assertFalse(Deals.isBargain(stats(100_00, 5), listing(90_00)));
    }

    @Test
    void implausiblyCheapIsNoBargain() {
        assertFalse(Deals.isBargain(stats(100_000_00, 5), listing(2_900_00)));
    }

    @Test
    void filledContainersHaveNoMarketRatio() {
        ItemView diamond = new ItemView("minecraft:diamond", 1, "Diamant", List.of(), Map.of(), Map.of(), null, null, List.of(), null, null, Map.of(), List.of());
        ItemView kit = new ItemView("minecraft:shulker_box", 1, "Kit", List.of(), Map.of(), Map.of(), null, null, List.of(), null, "Kit", Map.of(), List.of(diamond));
        Listing listing = new Listing(0, kit, ItemKeyFactory.of(kit), 10_000_000_00L, "seller", 3600, ListingStatus.ACTIVE, false);
        assertTrue(Deals.ratio(stats(2_900_00, 50), listing).isEmpty());
    }

    @Test
    void formatsDelta() {
        assertEquals("−12 %", Deals.deltaText(0.88));
        assertEquals("+30 %", Deals.deltaText(1.3));
        assertEquals("±0 %", Deals.deltaText(1.004));
    }
}
