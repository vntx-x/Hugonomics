package dev.hugomarket.module.ahsearch;

import dev.hugomarket.module.sync.PriceJson;

import java.util.List;
import java.util.OptionalDouble;

public final class PriceTrend {
    private PriceTrend() {
    }

    public static OptionalDouble ratio(List<PriceJson.HistoryPoint> points) {
        long first = 0;
        long last = 0;
        for (PriceJson.HistoryPoint point : points) {
            if (point.medianUnitCents() <= 0) {
                continue;
            }
            if (first == 0) {
                first = point.medianUnitCents();
            }
            last = point.medianUnitCents();
        }
        return first == 0 ? OptionalDouble.empty() : OptionalDouble.of((double) last / first);
    }
}
