package dev.hugomarket.module.deals;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.module.ahsearch.Deals;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.store.Fees;
import dev.hugomarket.store.PriceStats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class DealFinder {
    public record Deal(ListingIndex.Entry entry, double ratio, long profitCents) {
    }

    public enum Order {
        PROFIT,
        DISCOUNT
    }

    private DealFinder() {
    }

    public static List<Deal> find(List<ListingIndex.Entry> entries, Function<ItemKey, PriceStats> stats, Fees fees, String filter, Order order) {
        String needle = filter.trim().toLowerCase(Locale.ROOT);
        List<Deal> deals = new ArrayList<>();
        for (ListingIndex.Entry entry : entries) {
            if (!entry.listing().isMarketListing()) {
                continue;
            }
            if (!needle.isEmpty() && !entry.listing().item().displayName().toLowerCase(Locale.ROOT).contains(needle)
                    && !entry.listing().key().toString().contains(needle)) {
                continue;
            }
            PriceStats priceStats = stats.apply(entry.listing().key());
            double ratio = Deals.ratio(priceStats, entry.listing()).orElse(Double.NaN);
            if (Double.isNaN(ratio) || ratio > Deals.BARGAIN_FACTOR) {
                continue;
            }
            long resale = priceStats.medianUnitCents() * entry.listing().item().count();
            long profit = fees.netAfterSale(resale) - entry.listing().priceCents();
            if (profit <= 0) {
                continue;
            }
            deals.add(new Deal(entry, ratio, profit));
        }
        Comparator<Deal> comparator = order == Order.PROFIT
                ? Comparator.comparingLong(Deal::profitCents).reversed()
                : Comparator.comparingDouble(Deal::ratio);
        deals.sort(comparator.thenComparing(d -> d.entry().listing().priceCents()));
        return deals;
    }
}
