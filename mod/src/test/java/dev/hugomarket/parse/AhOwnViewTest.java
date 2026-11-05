package dev.hugomarket.parse;

import dev.hugomarket.model.AhView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AhOwnViewTest {
    private final AhListingParser parser = new AhListingParser(HugoGuiRules.load());

    @Test
    void marketPageIsAllView() {
        AhListingParser.AhPage page = parser.parse(DumpLoader.container("ah-list/ah-list-2.json"));
        assertEquals(AhView.ALL, page.view());
        assertTrue(page.isMarket());
        assertTrue(page.listings().stream().allMatch(Listing::isMarketListing));
    }

    @Test
    void runningOwnAuctionsAreOwnAndNotMarket() {
        AhListingParser.AhPage page = parser.parse(DumpLoader.container("ah-own-laufend/ah-own-laufend-1.json"));
        assertEquals(AhView.OWN_ACTIVE, page.view());
        assertFalse(page.isMarket());
        Listing first = page.listings().get(0);
        assertEquals("Du", first.seller());
        assertEquals(ListingStatus.ACTIVE, first.status());
        assertTrue(first.own());
        assertFalse(first.isMarketListing());
        assertEquals(1_000_000, first.priceCents());
        assertTrue(first.expiresInSeconds() > 0);
    }

    @Test
    void soldAuctionsCarryStatusWithoutExpiry() {
        AhListingParser.AhPage page = parser.parse(DumpLoader.container("ah-own-verkauft/ah-own-verkauft-1.json"));
        assertEquals(AhView.OWN_SOLD, page.view());
        Listing first = page.listings().get(0);
        assertEquals(ListingStatus.SOLD, first.status());
        assertEquals(20_000, first.priceCents());
        assertEquals(-1, first.expiresInSeconds());
        assertTrue(first.own());
    }

    @Test
    void boughtAuctionsKeepRealSellerButCountAsOwnHistory() {
        AhListingParser.AhPage page = parser.parse(DumpLoader.container("ah-own-gekauft/ah-own-gekauft-1.json"));
        assertEquals(AhView.OWN_BOUGHT, page.view());
        assertTrue(page.listings().size() >= 3);
        Listing first = page.listings().get(0);
        assertEquals(ListingStatus.BOUGHT, first.status());
        assertTrue(first.seller().startsWith("player_"));
        assertTrue(first.own());
        assertFalse(first.isMarketListing());
        assertEquals(5_000, first.priceCents());
        assertEquals(5, first.item().count());
    }
}
