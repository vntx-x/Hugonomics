package dev.hugomarket.store;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.Listing;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketSessionTest {
    private final AhListingParser parser = new AhListingParser(HugoGuiRules.load());

    @Test
    void medianAndLowestFromArrays() {
        PriceStats odd = PriceStats.of(new long[]{500, 100, 300}, new int[]{1, 2, 3}, 7);
        assertEquals(100, odd.lowestUnitCents());
        assertEquals(300, odd.medianUnitCents());
        assertEquals(3, odd.listings());
        assertEquals(6, odd.units());
        PriceStats even = PriceStats.of(new long[]{400, 100, 300, 200}, new int[]{1, 1, 1, 1}, 7);
        assertEquals(200, even.medianUnitCents());
        PriceStats rockets = PriceStats.of(new long[]{37, 37, 37, 999, 999, 999}, new int[]{64, 64, 22, 1, 1, 1}, 7);
        assertEquals(37, rockets.medianUnitCents());
        assertNull(PriceStats.of(new long[0], new int[0], 0));
        assertEquals(95, odd.suggestUndercut(0.95));
    }

    @Test
    void statsPerKeyFromSeenPages() {
        MarketSession session = new MarketSession(new PriceStore(java.nio.file.Path.of("build", "unused-cache.json")));
        List<Listing> page = parser.parse(DumpLoader.container("ah-list/ah-list-2.json")).listings();
        session.addPage(page, 1_000);
        PriceStats stars = session.stats(ItemKey.parse("nether_star"));
        assertNotNull(stars);
        assertEquals(80_000_000, stars.lowestUnitCents());
        assertEquals(80_000_000, stars.medianUnitCents());
        assertTrue(stars.listings() >= 20);
        assertEquals(1_000, stars.lastSeen());
        assertNull(session.stats(ItemKey.parse("dragon_egg")));
    }

    @Test
    void statsRefreshAfterNewPage() {
        MarketSession session = new MarketSession(new PriceStore(java.nio.file.Path.of("build", "unused-cache.json")));
        session.addPage(parser.parse(DumpLoader.container("ah-list/ah-list-1.json")).listings(), 1_000);
        int before = session.size();
        session.addPage(parser.parse(DumpLoader.container("ah-list/ah-list-2.json")).listings(), 2_000);
        assertTrue(session.size() > before);
        assertNotNull(session.stats(ItemKey.parse("nether_star")));
    }
}
