package dev.hugomarket.module.myauctions;

import dev.hugomarket.model.AhView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.store.Fees;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.PriceStats;

import java.util.List;

public record OwnAuctionSummary(
        AhView view,
        int count,
        long priceSumCents,
        long netSumCents,
        long marketSumCents,
        int withMarketPrice,
        int overpriced,
        long soonestExpirySeconds
) {
    private static final double OVERPRICED_FACTOR = 1.25;

    public static OwnAuctionSummary of(AhView view, List<Listing> listings, MarketSession market, Fees fees) {
        int count = 0;
        long priceSum = 0;
        long netSum = 0;
        long marketSum = 0;
        int withMarket = 0;
        int overpriced = 0;
        long soonest = -1;
        for (Listing listing : listings) {
            count++;
            priceSum += listing.priceCents();
            netSum += fees.netAfterSale(listing.priceCents());
            PriceStats stats = market.stats(listing.key());
            if (stats != null) {
                withMarket++;
                long marketStack = stats.lowestUnitCents() * listing.item().count();
                marketSum += marketStack;
                if (listing.priceCents() > marketStack * OVERPRICED_FACTOR) {
                    overpriced++;
                }
            }
            if (listing.expiresInSeconds() >= 0 && (soonest < 0 || listing.expiresInSeconds() < soonest)) {
                soonest = listing.expiresInSeconds();
            }
        }
        return new OwnAuctionSummary(view, count, priceSum, netSum, marketSum, withMarket, overpriced, soonest);
    }
}
