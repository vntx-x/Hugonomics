package dev.hugomarket.module.myauctions;

import dev.hugomarket.model.AhView;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.store.Fees;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.PriceStore;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnAuctionSummaryTest {
    private final AhListingParser parser = new AhListingParser(HugoGuiRules.load());
    private final MarketSession market = new MarketSession(new PriceStore(Path.of("build", "unused-cache.json")));
    private final Fees fees = Fees.load();

    @Test
    void summarisesRunningAuctions() {
        AhListingParser.AhPage page = parser.parse(DumpLoader.container("ah-own-laufend/ah-own-laufend-1.json"));
        OwnAuctionSummary summary = OwnAuctionSummary.of(page.view(), page.listings(), market, fees);
        assertEquals(AhView.OWN_ACTIVE, summary.view());
        assertEquals(1, summary.count());
        assertEquals(1_000_000, summary.priceSumCents());
        assertEquals(fees.netAfterSale(1_000_000), summary.netSumCents());
        assertEquals(0, summary.withMarketPrice());
        assertTrue(summary.soonestExpirySeconds() > 0);
    }

    @Test
    void summarisesSoldAuctionsWithoutExpiry() {
        AhListingParser.AhPage page = parser.parse(DumpLoader.container("ah-own-verkauft/ah-own-verkauft-1.json"));
        OwnAuctionSummary summary = OwnAuctionSummary.of(page.view(), page.listings(), market, fees);
        assertEquals(AhView.OWN_SOLD, summary.view());
        assertTrue(summary.count() >= 30);
        assertEquals(-1, summary.soonestExpirySeconds());
        assertTrue(summary.netSumCents() < summary.priceSumCents());
    }

    @Test
    void marksOverpricedAgainstMarket() {
        AhListingParser.AhPage sold = parser.parse(DumpLoader.container("ah-list/ah-list-2.json"));
        market.addPage(sold.listings(), 1_000);
        AhListingParser.AhPage page = parser.parse(DumpLoader.container("ah-list/ah-list-2.json"));
        OwnAuctionSummary summary = OwnAuctionSummary.of(AhView.OWN_ACTIVE, page.listings(), market, fees);
        assertEquals(page.listings().size(), summary.withMarketPrice());
        assertTrue(summary.overpriced() < summary.withMarketPrice());
        assertTrue(summary.marketSumCents() <= summary.priceSumCents());
    }
}
