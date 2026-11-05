package dev.hugomarket.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SellStatsTest {
    private static SellStats.Payout payout(long ts, long cents) {
        return new SellStats.Payout(ts, cents, cents, 0, 1.0);
    }

    @Test
    void aggregatesSessionDayAndRate(@TempDir Path dir) {
        long start = 1_000_000_000_000L;
        SellStats stats = new SellStats(dir, start);
        stats.record(payout(start + 10_000, 10_000));
        stats.record(payout(start + 20_000, 10_000));
        stats.record(payout(start + 130_000, 5_000));
        SellStats.Summary summary = stats.summary(start + 140_000);
        assertEquals(25_000, summary.sessionCents());
        assertEquals(25_000, summary.last24hCents());
        assertEquals(25_000, summary.allTimeCents());
        assertEquals(3, summary.allTimeCount());
        assertEquals(3, summary.activeMinutesSession());
        assertEquals(25_000 * 12, summary.ratePerHourCents());

        SellStats.Summary later = stats.summary(start + 140_000 + 6 * 60_000);
        assertEquals(0, later.ratePerHourCents());
        assertEquals(25_000, later.last24hCents());
    }

    @Test
    void persistsAcrossRestartsWithoutDoubleCounting(@TempDir Path dir) {
        long start = 1_000_000_000_000L;
        SellStats first = new SellStats(dir, start);
        first.record(payout(start + 1_000, 7_000));
        first.record(payout(start + 2_000, 3_000));

        long restart = start + 3 * 60 * 60_000;
        SellStats second = new SellStats(dir, restart);
        second.load(restart);
        SellStats.Summary summary = second.summary(restart);
        assertEquals(0, summary.sessionCents());
        assertEquals(10_000, summary.last24hCents());
        assertEquals(10_000, summary.allTimeCents());
        assertEquals(2, summary.allTimeCount());
        assertEquals(2, second.recent().size());

        second.record(payout(restart + 1_000, 1_000));
        assertEquals(11_000, second.summary(restart + 2_000).allTimeCents());
        assertEquals(1_000, second.summary(restart + 2_000).sessionCents());
    }

    @Test
    void oldPayoutsLeaveTheDayWindow(@TempDir Path dir) {
        long start = 1_000_000_000_000L;
        SellStats stats = new SellStats(dir, start);
        stats.record(payout(start, 5_000));
        SellStats.Summary summary = stats.summary(start + 25L * 60 * 60_000);
        assertEquals(0, summary.last24hCents());
        assertEquals(5_000, summary.allTimeCents());
    }
}
