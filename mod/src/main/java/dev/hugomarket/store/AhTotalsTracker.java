package dev.hugomarket.store;

import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AhTotalsTracker {
    public static final String KEY = "ah";

    private final Path file;
    private long lastEarnedCents = -1;
    private long lastSpentCents = -1;
    private long buysSinceCents;
    private long salesSinceCents;

    public AhTotalsTracker(Path file) {
        this.file = file;
    }

    public void load() {
        JsonObject root = JsonFile.read(file);
        if (root == null) {
            return;
        }
        lastEarnedCents = root.get("earned").getAsLong();
        lastSpentCents = root.get("spent").getAsLong();
        buysSinceCents = root.get("buysSince").getAsLong();
        salesSinceCents = root.has("salesSince") ? root.get("salesSince").getAsLong() : 0;
    }

    public List<TradeLog.Trade> observe(long earnedCents, long spentCents, long now) {
        List<TradeLog.Trade> trades = new ArrayList<>(2);
        if (lastEarnedCents >= 0 && earnedCents > lastEarnedCents) {
            long unaccounted = earnedCents - lastEarnedCents - salesSinceCents;
            if (unaccounted > 0) {
                trades.add(new TradeLog.Trade(now, TradeLog.Kind.AH_SELL, KEY, "", 0, unaccounted, ""));
            }
            salesSinceCents = 0;
        }
        if (lastSpentCents >= 0 && spentCents > lastSpentCents) {
            long unaccounted = spentCents - lastSpentCents - buysSinceCents;
            if (unaccounted > 0) {
                trades.add(new TradeLog.Trade(now, TradeLog.Kind.AH_BUY, KEY, "", 0, unaccounted, ""));
            }
            buysSinceCents = 0;
        }
        boolean changed = earnedCents != lastEarnedCents || spentCents != lastSpentCents;
        lastEarnedCents = earnedCents;
        lastSpentCents = spentCents;
        if (changed) {
            save();
        }
        return trades;
    }

    public void noteBuy(long priceCents) {
        buysSinceCents += priceCents;
        save();
    }

    public void noteSale(long priceCents) {
        salesSinceCents += priceCents;
        save();
    }

    private void save() {
        JsonObject root = new JsonObject();
        root.addProperty("earned", lastEarnedCents);
        root.addProperty("spent", lastSpentCents);
        root.addProperty("buysSince", buysSinceCents);
        root.addProperty("salesSince", salesSinceCents);
        JsonFile.write(file, root);
    }
}
