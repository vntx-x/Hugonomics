package dev.hugomarket.store;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hugomarket.HugoMarket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class SellStats {
    public record Payout(long ts, long totalCents, long baseCents, long bonusCents, double multiplier) {
    }

    public record Summary(long sessionCents, long last24hCents, long allTimeCents, long allTimeCount, long ratePerHourCents,
                          int activeMinutesSession) {
    }

    private static final long DAY_MS = 24 * 60 * 60_000;
    private static final long RATE_WINDOW_MS = 5 * 60_000;
    private static final long KEEP_MS = 31 * DAY_MS;

    private final Path logFile;
    private final Path summaryFile;
    private final Deque<Payout> recent = new ArrayDeque<>();
    private final long sessionStart;
    private long allTimeCents;
    private long allTimeCount;
    private long sessionCents;
    private long lastActiveMinute = Long.MIN_VALUE;
    private int activeMinutes;
    private final List<java.util.function.Consumer<Payout>> listeners = new java.util.ArrayList<>();

    public void onPayout(java.util.function.Consumer<Payout> listener) {
        listeners.add(listener);
    }

    public SellStats(Path dir, long now) {
        this.logFile = dir.resolve("payouts.jsonl");
        this.summaryFile = dir.resolve("payouts-summary.json");
        this.sessionStart = now;
    }

    public void load(long now) {
        JsonObject summary = JsonFile.read(summaryFile);
        if (summary != null) {
            allTimeCents = summary.get("allTimeCents").getAsLong();
            allTimeCount = summary.get("allTimeCount").getAsLong();
        }
        if (!Files.exists(logFile)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                JsonObject p = JsonParser.parseString(line).getAsJsonObject();
                long ts = p.get("ts").getAsLong();
                if (now - ts <= KEEP_MS) {
                    recent.addLast(new Payout(ts, p.get("total").getAsLong(), p.get("base").getAsLong(), p.get("bonus").getAsLong(),
                            p.get("multiplier").getAsDouble()));
                }
            }
        } catch (IOException | RuntimeException e) {
            HugoMarket.LOGGER.warn("payout log unreadable: {}", e.toString());
        }
    }

    public void record(Payout payout) {
        recent.addLast(payout);
        sessionCents += payout.totalCents();
        allTimeCents += payout.totalCents();
        allTimeCount++;
        long minute = payout.ts() / 60_000;
        if (minute != lastActiveMinute) {
            lastActiveMinute = minute;
            activeMinutes++;
        }
        while (!recent.isEmpty() && payout.ts() - recent.peekFirst().ts() > KEEP_MS) {
            recent.pollFirst();
        }
        append(payout);
        for (java.util.function.Consumer<Payout> listener : listeners) {
            listener.accept(payout);
        }
    }

    public Summary summary(long now) {
        long last24h = 0;
        long window = 0;
        long windowStart = Long.MAX_VALUE;
        for (Payout payout : recent) {
            if (now - payout.ts() <= DAY_MS) {
                last24h += payout.totalCents();
            }
            if (now - payout.ts() <= RATE_WINDOW_MS) {
                window += payout.totalCents();
                windowStart = Math.min(windowStart, payout.ts());
            }
        }
        long rate = window == 0 ? 0 : Math.round(window * (3_600_000.0 / RATE_WINDOW_MS));
        return new Summary(sessionCents, last24h, allTimeCents, allTimeCount, rate, activeMinutes);
    }

    public List<Payout> recent() {
        return List.copyOf(recent);
    }

    public long sessionStart() {
        return sessionStart;
    }

    private void append(Payout payout) {
        JsonObject p = new JsonObject();
        p.addProperty("ts", payout.ts());
        p.addProperty("total", payout.totalCents());
        p.addProperty("base", payout.baseCents());
        p.addProperty("bonus", payout.bonusCents());
        p.addProperty("multiplier", payout.multiplier());
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, p + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            HugoMarket.LOGGER.error("payout log append failed", e);
        }
        JsonObject summary = new JsonObject();
        summary.addProperty("allTimeCents", allTimeCents);
        summary.addProperty("allTimeCount", allTimeCount);
        JsonFile.write(summaryFile, summary);
    }
}
