package dev.hugomarket.store;

import java.util.OptionalLong;

public final class Flip {
    public static final int MIN_LISTINGS = 3;

    private Flip() {
    }

    public static OptionalLong viaAh(long priceCents, int count, PriceStats stats, boolean filled, Fees fees) {
        if (stats == null || filled || stats.listings() < MIN_LISTINGS || stats.medianUnitCents() <= 0) {
            return OptionalLong.empty();
        }
        long resale = stats.medianUnitCents() * Math.max(1, count);
        return OptionalLong.of(fees.netAfterSale(resale) - priceCents);
    }

    public static OptionalLong viaOrder(long priceCents, int count, OrderBook.BestOrder order) {
        if (order == null || order.openUnits() < Math.max(1, count)) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(order.unitPriceCents() * Math.max(1, count) - priceCents);
    }
}
