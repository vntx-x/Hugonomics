package dev.hugomarket.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hugomarket.HugoMarket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class PriceStore {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Path file;
    private final Map<String, PriceStats> prices = new HashMap<>();
    private long savedAt;
    private boolean dirty;

    public PriceStore(Path file) {
        this.file = file;
    }

    public PriceStats get(String key) {
        return prices.get(key);
    }

    public int size() {
        return prices.size();
    }

    public long savedAt() {
        return savedAt;
    }

    public void put(String key, PriceStats stats) {
        if (stats == null) {
            prices.remove(key);
        } else {
            prices.put(key, stats);
        }
        dirty = true;
    }

    public void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            savedAt = root.has("savedAt") ? root.get("savedAt").getAsLong() : 0;
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("prices").entrySet()) {
                JsonObject p = entry.getValue().getAsJsonObject();
                prices.put(entry.getKey(), new PriceStats(p.get("lowest").getAsLong(), p.get("median").getAsLong(),
                        p.get("listings").getAsInt(), p.get("units").getAsInt(), p.get("lastSeen").getAsLong()));
            }
        } catch (IOException | RuntimeException e) {
            HugoMarket.LOGGER.warn("price cache unreadable: {}", e.toString());
        }
    }

    public void saveIfDirty() {
        if (!dirty) {
            return;
        }
        dirty = false;
        JsonObject root = new JsonObject();
        savedAt = System.currentTimeMillis();
        root.addProperty("savedAt", savedAt);
        JsonObject all = new JsonObject();
        for (Map.Entry<String, PriceStats> entry : prices.entrySet()) {
            PriceStats s = entry.getValue();
            JsonObject p = new JsonObject();
            p.addProperty("lowest", s.lowestUnitCents());
            p.addProperty("median", s.medianUnitCents());
            p.addProperty("listings", s.listings());
            p.addProperty("units", s.units());
            p.addProperty("lastSeen", s.lastSeen());
            all.add(entry.getKey(), p);
        }
        root.add("prices", all);
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            HugoMarket.LOGGER.error("price cache save failed", e);
        }
    }
}
