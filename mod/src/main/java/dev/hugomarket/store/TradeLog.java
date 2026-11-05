package dev.hugomarket.store;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hugomarket.HugoMarket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class TradeLog {
    public enum Kind {
        AH_BUY,
        AH_SELL,
        SHOP_SELL
    }

    public record Trade(long ts, Kind kind, String key, String displayName, int count, long priceCents, String counterparty) {
        public boolean isIncome() {
            return kind != Kind.AH_BUY;
        }
    }

    public record Summary(long spentCents, long incomeCents, int buys, int sells) {
        public long netCents() {
            return incomeCents - spentCents;
        }
    }

    private static final long KEEP_MS = 31L * 24 * 60 * 60_000;

    private final Path file;
    private final Deque<Trade> trades = new ArrayDeque<>();

    public TradeLog(Path file) {
        this.file = file;
    }

    public void load(long now) {
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                JsonObject t = JsonParser.parseString(line).getAsJsonObject();
                long ts = t.get("ts").getAsLong();
                if (now - ts > KEEP_MS) {
                    continue;
                }
                trades.addLast(new Trade(ts, Kind.valueOf(t.get("kind").getAsString()), t.get("key").getAsString(),
                        t.get("name").getAsString(), t.get("count").getAsInt(), t.get("price").getAsLong(),
                        t.has("counterparty") ? t.get("counterparty").getAsString() : ""));
            }
        } catch (IOException | RuntimeException e) {
            HugoMarket.LOGGER.warn("trade log unreadable: {}", e.toString());
        }
    }

    public void record(Trade trade) {
        trades.addLast(trade);
        while (!trades.isEmpty() && trade.ts() - trades.peekFirst().ts() > KEEP_MS) {
            trades.pollFirst();
        }
        JsonObject t = new JsonObject();
        t.addProperty("ts", trade.ts());
        t.addProperty("kind", trade.kind().name());
        t.addProperty("key", trade.key());
        t.addProperty("name", trade.displayName());
        t.addProperty("count", trade.count());
        t.addProperty("price", trade.priceCents());
        t.addProperty("counterparty", trade.counterparty());
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, t + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            HugoMarket.LOGGER.error("trade log append failed", e);
        }
    }

    public List<Trade> recent(int max) {
        List<Trade> out = new java.util.ArrayList<>(Math.min(max, trades.size()));
        var it = trades.descendingIterator();
        while (it.hasNext() && out.size() < max) {
            out.add(it.next());
        }
        return out;
    }

    public Summary since(long from) {
        long spent = 0;
        long income = 0;
        int buys = 0;
        int sells = 0;
        for (Trade trade : trades) {
            if (trade.ts() < from) {
                continue;
            }
            if (trade.isIncome()) {
                income += trade.priceCents();
                sells++;
            } else {
                spent += trade.priceCents();
                buys++;
            }
        }
        return new Summary(spent, income, buys, sells);
    }

    public Summary today(long now, ZoneId zone) {
        long startOfDay = LocalDate.ofInstant(Instant.ofEpochMilli(now), zone).atStartOfDay(zone).toInstant().toEpochMilli();
        return since(startOfDay);
    }

    public int size() {
        return trades.size();
    }
}
