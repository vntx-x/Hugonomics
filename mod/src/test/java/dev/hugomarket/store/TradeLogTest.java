package dev.hugomarket.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradeLogTest {
    private static TradeLog.Trade trade(long ts, TradeLog.Kind kind, long price) {
        return new TradeLog.Trade(ts, kind, "emerald", "Emerald", 1, price, "x");
    }

    @Test
    void summarisesIncomeAndSpendingPerWindow(@TempDir Path dir) {
        long now = 1_762_300_000_000L;
        TradeLog log = new TradeLog(dir.resolve("trades.jsonl"));
        log.record(trade(now - 10 * 24 * 3600_000L, TradeLog.Kind.AH_BUY, 1_000));
        log.record(trade(now - 2 * 24 * 3600_000L, TradeLog.Kind.AH_BUY, 2_000));
        log.record(trade(now - 3600_000L, TradeLog.Kind.SHOP_SELL, 5_000));
        log.record(trade(now - 60_000L, TradeLog.Kind.AH_SELL, 7_000));

        TradeLog.Summary week = log.since(now - 7 * 24 * 3600_000L);
        assertEquals(2_000, week.spentCents());
        assertEquals(12_000, week.incomeCents());
        assertEquals(10_000, week.netCents());
        assertEquals(1, week.buys());
        assertEquals(2, week.sells());

        TradeLog.Summary month = log.since(now - 30 * 24 * 3600_000L);
        assertEquals(3_000, month.spentCents());
        assertEquals(4, log.size());
        assertEquals(0, log.today(now + 2 * 24 * 3600_000L, ZoneOffset.UTC).sells());
    }

    @Test
    void sumsNetPerCalendarDay(@TempDir Path dir) {
        long midnight = 1_762_300_800_000L;
        long now = midnight + 3600_000L;
        TradeLog log = new TradeLog(dir.resolve("trades.jsonl"));
        log.record(trade(midnight - 20 * 24 * 3600_000L, TradeLog.Kind.AH_SELL, 9_999));
        log.record(trade(midnight - 1_000, TradeLog.Kind.AH_BUY, 3_000));
        log.record(trade(midnight - 2_000, TradeLog.Kind.AH_SELL, 1_000));
        log.record(trade(midnight + 1_000, TradeLog.Kind.SHOP_SELL, 4_000));
        long[] net = log.dailyNet(now, ZoneOffset.UTC, 3);
        assertEquals(3, net.length);
        assertEquals(0, net[0]);
        assertEquals(-2_000, net[1]);
        assertEquals(4_000, net[2]);
    }

    @Test
    void persistsAndKeepsOnlyLastMonth(@TempDir Path dir) {
        long now = 1_762_300_000_000L;
        TradeLog log = new TradeLog(dir.resolve("trades.jsonl"));
        log.record(trade(now - 40L * 24 * 3600_000L, TradeLog.Kind.AH_BUY, 100));
        log.record(trade(now - 1_000, TradeLog.Kind.AH_BUY, 200));
        TradeLog reloaded = new TradeLog(dir.resolve("trades.jsonl"));
        reloaded.load(now);
        assertEquals(1, reloaded.size());
        assertEquals(200, reloaded.recent(5).get(0).priceCents());
    }
}
