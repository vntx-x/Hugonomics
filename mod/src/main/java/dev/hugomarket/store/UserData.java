package dev.hugomarket.store;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

public final class UserData {
    private static final int MAX_PRICES = 500;

    private final Path file;
    private final Set<String> watchlist = new LinkedHashSet<>();
    private final Map<String, Long> lastPrices = new LinkedHashMap<>();
    private final Map<String, Long> alerts = new LinkedHashMap<>();

    public UserData(Path file) {
        this.file = file;
    }

    public void load() {
        JsonObject root = JsonFile.read(file);
        if (root == null) {
            return;
        }
        if (root.has("watchlist")) {
            for (JsonElement key : root.getAsJsonArray("watchlist")) {
                watchlist.add(key.getAsString());
            }
        }
        if (root.has("lastUnitPrices")) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("lastUnitPrices").entrySet()) {
                lastPrices.put(entry.getKey(), entry.getValue().getAsLong());
            }
        }
        if (root.has("alerts")) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("alerts").entrySet()) {
                alerts.put(entry.getKey(), entry.getValue().getAsLong());
            }
        }
    }

    public Map<String, Long> alerts() {
        return alerts;
    }

    public void setAlert(String key, long unitCents) {
        if (unitCents <= 0) {
            alerts.remove(key);
        } else {
            alerts.put(key, unitCents);
        }
        save();
    }

    public OptionalLong alert(String key) {
        Long cents = alerts.get(key);
        return cents == null ? OptionalLong.empty() : OptionalLong.of(cents);
    }

    public void save() {
        JsonObject root = new JsonObject();
        JsonArray watch = new JsonArray();
        watchlist.forEach(watch::add);
        root.add("watchlist", watch);
        JsonObject prices = new JsonObject();
        lastPrices.forEach(prices::addProperty);
        root.add("lastUnitPrices", prices);
        JsonObject alertJson = new JsonObject();
        alerts.forEach(alertJson::addProperty);
        root.add("alerts", alertJson);
        JsonFile.write(file, root);
    }

    public boolean isWatched(String key) {
        return watchlist.contains(key);
    }

    public boolean toggleWatch(String key) {
        boolean watched = !watchlist.remove(key);
        if (watched) {
            watchlist.add(key);
        }
        save();
        return watched;
    }

    public Set<String> watchlist() {
        return watchlist;
    }

    public void rememberUnitPrice(String key, long unitCents) {
        Long previous = lastPrices.get(key);
        if (previous != null && previous == unitCents) {
            return;
        }
        lastPrices.remove(key);
        lastPrices.put(key, unitCents);
        while (lastPrices.size() > MAX_PRICES) {
            lastPrices.remove(lastPrices.keySet().iterator().next());
        }
        save();
    }

    public OptionalLong lastUnitPrice(String key) {
        Long cents = lastPrices.get(key);
        return cents == null ? OptionalLong.empty() : OptionalLong.of(cents);
    }
}
