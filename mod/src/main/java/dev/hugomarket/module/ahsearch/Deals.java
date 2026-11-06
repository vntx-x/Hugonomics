package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Listing;
import dev.hugomarket.store.PriceStats;

import java.util.Locale;
import java.util.OptionalDouble;

public final class Deals {
    public static final double BARGAIN_FACTOR = 0.85;
    public static final double IMPLAUSIBLE_FACTOR = 0.2;
    public static final int MIN_LISTINGS = 3;

    private Deals() {
    }

    public static OptionalDouble ratio(PriceStats stats, Listing listing) {
        if (stats == null || listing.key().filled() || stats.listings() < MIN_LISTINGS || stats.medianUnitCents() <= 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of((double) listing.unitPriceCents() / stats.medianUnitCents());
    }

    public static boolean isBargain(double ratio) {
        return ratio >= IMPLAUSIBLE_FACTOR && ratio <= BARGAIN_FACTOR;
    }

    public static boolean isBargain(PriceStats stats, Listing listing) {
        OptionalDouble ratio = ratio(stats, listing);
        return ratio.isPresent() && isBargain(ratio.getAsDouble());
    }

    public static String deltaText(double ratio) {
        long percent = Math.round((ratio - 1) * 100);
        if (percent == 0) {
            return "±0 %";
        }
        return String.format(Locale.ROOT, "%s%d %%", percent > 0 ? "+" : "−", Math.abs(percent));
    }
}
