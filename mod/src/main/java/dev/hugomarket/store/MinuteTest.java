package dev.hugomarket.store;

import java.util.List;

public final class MinuteTest {
    public record Result(String name, long finishedAt, long baseCents, long bonusCents, long totalCents, double multiplier, int sales) {
        public long projected(double targetMultiplier) {
            return Math.round(baseCents * targetMultiplier);
        }

        public static Result named(Result r, String name) {
            return new Result(name, r.finishedAt, r.baseCents, r.bonusCents, r.totalCents, r.multiplier, r.sales);
        }
    }

    public static final long TARGET_ACTIVE_MS = 60_000;
    private static final long ACTIVE_WINDOW_MS = 5_000;
    public static final List<Double> MULTIPLIERS = List.of(1.0, 1.1, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0);

    private boolean running;
    private long lastSaleAt;
    private long lastTickAt;
    private long activeMs;
    private long baseCents;
    private long bonusCents;
    private long totalCents;
    private double multiplier = 1.0;
    private int sales;
    private Result finished;

    public void start(long now) {
        running = true;
        lastSaleAt = Long.MIN_VALUE;
        lastTickAt = now;
        activeMs = 0;
        baseCents = 0;
        bonusCents = 0;
        totalCents = 0;
        multiplier = 1.0;
        sales = 0;
        finished = null;
    }

    public void cancel() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public long activeMs() {
        return activeMs;
    }

    public long baseCentsSoFar() {
        return baseCents;
    }

    public Result finished() {
        return finished;
    }

    public void sale(long now, long base, long bonus, long total, double observedMultiplier) {
        if (!running) {
            return;
        }
        tick(now);
        lastSaleAt = now;
        baseCents += base;
        bonusCents += bonus;
        totalCents += total;
        multiplier = observedMultiplier;
        sales++;
    }

    public void tick(long now) {
        if (!running) {
            return;
        }
        long elapsed = Math.max(0, now - lastTickAt);
        lastTickAt = now;
        boolean selling = lastSaleAt != Long.MIN_VALUE && now - lastSaleAt <= ACTIVE_WINDOW_MS;
        if (selling) {
            activeMs += elapsed;
        }
        if (activeMs >= TARGET_ACTIVE_MS) {
            running = false;
            finished = new Result("", now, baseCents, bonusCents, totalCents, multiplier, sales);
        }
    }
}
