package dev.hugomarket.store;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.parse.HugoLore;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerValuationTest {
    private final AhListingParser parser = new AhListingParser(HugoGuiRules.load());

    @Test
    void sumsHugoWorthLinesOfAChest() {
        ContainerView chest = DumpLoader.container("chest/chest-1.json");
        ContainerValuation value = ContainerValuation.of(chest, new MarketSession());
        long expected = chest.containerItems()
                .mapToLong(s -> HugoLore.worth(s.item().lore()).map(HugoLore.Worth::valueCents).orElse(0L)).sum();
        assertEquals(expected, value.hugoWorthCents());
        assertTrue(value.hugoBaseCents() < value.hugoWorthCents());
        assertEquals(18, value.items());
        assertEquals(0, value.valuedByAh());
    }

    @Test
    void valuesListingsWithSessionPrices() {
        ContainerView page = DumpLoader.container("ah-list/ah-list-2.json");
        List<Listing> listings = parser.parse(page).listings();
        MarketSession market = new MarketSession();
        market.addPage(listings, 1_000);
        ContainerValuation value = ContainerValuation.of(page, market);
        long expectedLowest = 0;
        for (Listing listing : listings) {
            expectedLowest += market.stats(listing.key()).lowestUnitCents() * listing.item().count();
        }
        assertEquals(expectedLowest, value.ahLowestCents());
        assertEquals(listings.size(), value.valuedByAh());
        assertTrue(value.ahMedianCents() >= value.ahLowestCents());
    }

    @Test
    void shulkerContentsAreValuedWhenBoxItselfHasNoPrice() {
        ContainerView enderchest = DumpLoader.container("enderchest/enderchest-1.json");
        MarketSession market = new MarketSession();
        ContainerValuation value = ContainerValuation.of(enderchest, market);
        assertTrue(value.hugoWorthCents() > 0);
        assertTrue(value.unvaluedByAh() > 0);
        assertEquals(0, value.valuedByAh());
    }
}
