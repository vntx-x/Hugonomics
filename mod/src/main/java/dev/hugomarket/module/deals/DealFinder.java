package dev.hugomarket.module.deals;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.Listing;
import dev.hugomarket.module.ahsearch.Deals;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.store.Fees;
import dev.hugomarket.store.Flip;
import dev.hugomarket.store.OrderBook;
import dev.hugomarket.store.PriceStats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.function.Function;

public final class DealFinder {
    public record Deal(ListingIndex.Entry entry, double ratio, long profitCents, boolean viaOrder) {
    }

    public enum Order {
        PROFIT,
        DISCOUNT
    }

    private DealFinder() {
    }

    public static List<Deal> find(List<ListingIndex.Entry> entries, Function<ItemKey, PriceStats> stats,
                                  Function<String, OrderBook.BestOrder> orders, Fees fees, String filter, Order order) {
        String needle = filter.trim().toLowerCase(Locale.ROOT);
        List<Deal> deals = new ArrayList<>();
        for (ListingIndex.Entry entry : entries) {
            Listing listing = entry.listing();
            if (!listing.isMarketListing()) {
                continue;
            }
            if (!needle.isEmpty() && !listing.item().displayName().toLowerCase(Locale.ROOT).contains(needle)
                    && !listing.key().toString().contains(needle)) {
                continue;
            }
            PriceStats priceStats = stats.apply(listing.key());
            double ratio = Deals.ratio(priceStats, listing).orElse(Double.NaN);
            long ahProfit = 0;
            if (!Double.isNaN(ratio) && Deals.isBargain(ratio)) {
                ahProfit = Flip.viaAh(listing.priceCents(), listing.item().count(), priceStats, listing.key().filled(), fees).orElse(0);
            }
            OptionalLong viaOrder = Flip.viaOrder(listing.priceCents(), listing.item().count(), orders.apply(listing.key().toString()));
            long orderProfit = viaOrder.orElse(0);
            if (ahProfit <= 0 && orderProfit <= 0) {
                continue;
            }
            boolean useOrder = orderProfit > ahProfit;
            deals.add(new Deal(entry, ratio, useOrder ? orderProfit : ahProfit, useOrder));
        }
        Comparator<Deal> comparator = order == Order.PROFIT
                ? Comparator.comparingLong(Deal::profitCents).reversed()
                : Comparator.comparingDouble(Deal::ratio);
        deals.sort(comparator.thenComparing(d -> d.entry().listing().priceCents()));
        return deals;
    }
}
