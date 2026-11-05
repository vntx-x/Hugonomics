package dev.hugomarket.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AhTotalsTrackerTest {
    @TempDir
    Path dir;

    @Test
    void firstObservationOnlyRemembers() {
        AhTotalsTracker tracker = new AhTotalsTracker(dir.resolve("t.json"));
        assertTrue(tracker.observe(697_208_300L, 745_449_500L, 1_000).isEmpty());
    }

    @Test
    void earnedIncreaseBecomesSale() {
        AhTotalsTracker tracker = new AhTotalsTracker(dir.resolve("t.json"));
        tracker.observe(100_00, 50_00, 1_000);
        List<TradeLog.Trade> trades = tracker.observe(160_00, 50_00, 2_000);
        assertEquals(1, trades.size());
        assertEquals(TradeLog.Kind.AH_SELL, trades.get(0).kind());
        assertEquals(60_00, trades.get(0).priceCents());
        assertTrue(trades.get(0).isIncome());
    }

    @Test
    void spentIncreaseIsReducedByRecordedBuys() {
        AhTotalsTracker tracker = new AhTotalsTracker(dir.resolve("t.json"));
        tracker.observe(0, 100_00, 1_000);
        tracker.noteBuy(30_00);
        List<TradeLog.Trade> trades = tracker.observe(0, 150_00, 2_000);
        assertEquals(1, trades.size());
        assertEquals(TradeLog.Kind.AH_BUY, trades.get(0).kind());
        assertEquals(20_00, trades.get(0).priceCents());
        assertTrue(tracker.observe(0, 150_00, 3_000).isEmpty());
    }

    @Test
    void fullyRecordedBuysProduceNothing() {
        AhTotalsTracker tracker = new AhTotalsTracker(dir.resolve("t.json"));
        tracker.observe(0, 100_00, 1_000);
        tracker.noteBuy(50_00);
        assertTrue(tracker.observe(0, 150_00, 2_000).isEmpty());
    }

    @Test
    void statePersists() {
        Path file = dir.resolve("t.json");
        new AhTotalsTracker(file).observe(10_00, 5_00, 1_000);
        AhTotalsTracker reloaded = new AhTotalsTracker(file);
        reloaded.load();
        List<TradeLog.Trade> trades = reloaded.observe(25_00, 5_00, 2_000);
        assertEquals(15_00, trades.get(0).priceCents());
    }
}
