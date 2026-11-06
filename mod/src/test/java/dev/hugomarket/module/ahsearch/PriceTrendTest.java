package dev.hugomarket.module.ahsearch;

import dev.hugomarket.module.sync.PriceJson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceTrendTest {
    @Test
    void comparesFirstAndLastMedian() {
        List<PriceJson.HistoryPoint> points = List.of(
                new PriceJson.HistoryPoint(1, 90, 0, 0),
                new PriceJson.HistoryPoint(2, 90, 100, 3),
                new PriceJson.HistoryPoint(3, 100, 112, 4));
        assertEquals(1.12, PriceTrend.ratio(points).getAsDouble(), 1e-9);
    }

    @Test
    void emptyWithoutMedians() {
        assertTrue(PriceTrend.ratio(List.of()).isEmpty());
        assertTrue(PriceTrend.ratio(List.of(new PriceJson.HistoryPoint(1, 5, 0, 0))).isEmpty());
    }
}
