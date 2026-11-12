package dev.hugomarket.store;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.model.SlotView;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.parse.HugoLore;
import dev.hugomarket.parse.ItemKeyFactory;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerValuationTest {
    private final AhListingParser parser = new AhListingParser(HugoGuiRules.load());

    @Test
    void sumsHugoWorthLinesOfAChest() {
        ContainerView chest = DumpLoader.container("chest/chest-1.json");
        ContainerValuation value = ContainerValuation.of(chest, new MarketSession(new PriceStore(java.nio.file.Path.of("build", "unused-cache.json"))));
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
        MarketSession market = new MarketSession(new PriceStore(java.nio.file.Path.of("build", "unused-cache.json")));
        market.addPage(listings, 1_000);
        ContainerValuation value = ContainerValuation.of(page, market);
        long expectedLowest = 0;
        int plain = 0;
        for (Listing listing : listings) {
            if (listing.item().hasContents()) {
                expectedLowest += ContainerValuation.ofItem(listing.item(), market).ahLowestCents();
            } else {
                expectedLowest += market.stats(listing.key()).lowestUnitCents() * listing.item().count();
                plain++;
            }
        }
        assertTrue(plain < listings.size());
        assertEquals(expectedLowest, value.ahLowestCents());
        assertTrue(value.valuedByAh() >= plain);
        assertTrue(value.ahMedianCents() >= value.ahLowestCents());
    }

    private static ItemView item(String id, int count, List<ItemView> contents) {
        return new ItemView("minecraft:" + id, count, id, List.of(), Map.of(), Map.of(), null, null, List.of(), null, null, Map.of(), contents);
    }

    private static Listing listing(ItemView item, long priceCents, String seller) {
        return new Listing(0, item, ItemKeyFactory.of(item), priceCents, seller, 3600, ListingStatus.ACTIVE, false);
    }

    @Test
    void filledShulkerIsValuedByContentsAndEmptyBoxInsteadOfKitPrices() {
        ItemView bones = item("bone", 64, List.of());
        ItemView filled = item("shulker_box", 1, List.of(bones, bones));
        MarketSession market = new MarketSession(new PriceStore(java.nio.file.Path.of("build", "unused-cache.json")));
        market.addPage(List.of(
                listing(bones, 64_00, "seller_1"),
                listing(filled, 1_000_000_00, "seller_2"),
                listing(item("shulker_box", 1, List.of()), 50_00, "seller_3")), 1_000);
        ContainerValuation value = ContainerValuation.ofItem(filled, market);
        assertEquals(50_00 + 128 * 1_00, value.ahLowestCents());
        assertEquals(3, value.valuedByAh());
    }

    @Test
    void stackedSpawnersCountEverySpawnerInTheStack() {
        ContainerView enderchest = DumpLoader.container("enderchest/enderchest-1.json");
        List<ItemView> spawners = enderchest.containerItems().map(SlotView::item)
                .filter(item -> item.id().equals("minecraft:spawner")).toList();
        assertEquals(List.of(1, 8), spawners.stream().map(ItemView::count).toList());
        assertEquals(ItemKeyFactory.of(spawners.get(0)), ItemKeyFactory.of(spawners.get(1)));
    }

    @Test
    void shulkerContentsAreValuedWhenBoxItselfHasNoPrice() {
        ContainerView enderchest = DumpLoader.container("enderchest/enderchest-1.json");
        MarketSession market = new MarketSession(new PriceStore(java.nio.file.Path.of("build", "unused-cache.json")));
        ContainerValuation value = ContainerValuation.of(enderchest, market);
        assertTrue(value.hugoWorthCents() > 0);
        assertTrue(value.unvaluedByAh() > 0);
        assertEquals(0, value.valuedByAh());
    }
}
