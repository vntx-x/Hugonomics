package dev.hugomarket.module.deals;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.store.Fees;
import dev.hugomarket.store.PriceStats;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DealFinderTest {
    private static final Fees FEES = new Fees(1, List.of(new Fees.Tier(1_000_000, 1), new Fees.Tier(-1, 2)), 1);

    private static ListingIndex.Entry entry(String id, String name, int count, long priceCents) {
        ItemView item = new ItemView("minecraft:" + id, count, name, List.of(), Map.of(), Map.of(), null, null, List.of(), null, null, Map.of(), List.of());
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
    void findsBargainsWithProfitAfterTax() {
        List<ListingIndex.Entry> entries = List.of(
                entry("diamond", "Diamant", 2, 120_00),
                entry("diamond", "Diamant", 1, 95_00),
                entry("emerald", "Smaragd", 10, 300_00),
                entry("gold", "Gold", 1, 1_00));
        List<DealFinder.Deal> deals = DealFinder.find(entries, DealFinderTest::stats, FEES, "", DealFinder.Order.PROFIT);
        assertEquals(2, deals.size());
        assertEquals("Smaragd", deals.get(0).entry().listing().item().displayName());
        assertEquals(500_00 - 5_00 - 300_00, deals.get(0).profitCents());
        assertEquals(0.6, deals.get(1).ratio(), 1e-9);
    }

    @Test
    void discountOrderAndFilter() {
        List<ListingIndex.Entry> entries = List.of(entry("diamond", "Diamant", 2, 120_00), entry("emerald", "Smaragd", 10, 300_00));
        List<DealFinder.Deal> byDiscount = DealFinder.find(entries, DealFinderTest::stats, FEES, "", DealFinder.Order.DISCOUNT);
        assertEquals("Diamant", byDiscount.get(0).entry().listing().item().displayName());
        assertTrue(DealFinder.find(entries, DealFinderTest::stats, FEES, "smar", DealFinder.Order.PROFIT).size() == 1);
    }
}
