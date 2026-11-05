package dev.hugomarket.parse;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AhListingParserTest {
    private final HugoGuiRules rules = HugoGuiRules.load();
    private final AhListingParser parser = new AhListingParser(rules);

    @Test
    void parsesEveryListingSlotOnPage() {
        ContainerView view = DumpLoader.container("ah-list/ah-list-2.json");
        AhListingParser.AhPage page = parser.parse(view);
        long contentSlots = view.containerItems().filter(s -> s.index() <= 44).count();
        assertEquals(contentSlots, page.listings().size());
        assertTrue(page.listings().size() >= 30);

        Listing first = page.listings().get(0);
        assertEquals(0, first.slot());
        assertEquals("minecraft:nether_star", first.item().id());
        assertEquals(80_000_000, first.priceCents());
        assertEquals(80_000_000, first.unitPriceCents());
        assertTrue(first.seller().startsWith("player_"));
        assertEquals(8 * 3600 + 33 * 60 + 43, first.expiresInSeconds());
        assertEquals("nether_star", first.key().toString());
    }

    @Test
    void readsFilterStateAndTotals() {
        AhListingParser.AhPage page = parser.parse(DumpLoader.container("ah-list/ah-list-1.json"));
        assertNotNull(page.filter());
        assertNotNull(page.filter().sort());
        assertNotNull(page.filter().stackFilter());
        assertNotNull(page.totals());
        assertEquals(745_449_500L, page.totals().spentCents());
        assertEquals(697_208_300L, page.totals().earnedCents());
    }

    @Test
    void unitPriceDividesByStackSize() {
        for (Listing listing : parser.parse(DumpLoader.container("ah-list/ah-list-1.json")).listings()) {
            assertEquals(Math.round((double) listing.priceCents() / listing.item().count()), listing.unitPriceCents());
            assertTrue(listing.seller().startsWith("player_"));
        }
    }
}
