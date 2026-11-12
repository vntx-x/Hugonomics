package dev.hugomarket.store;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hugomarket.model.ItemKey;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;

public record SpawnerPrices(String updatedAt, String source, Map<String, Long> unitCentsByType) {
    public static final SpawnerPrices EMPTY = new SpawnerPrices("", "", Map.of());
    private static final String SUFFIX = "_spawner";

    public static SpawnerPrices parse(JsonObject json) {
        Map<String, Long> prices = new LinkedHashMap<>();
        for (JsonElement element : json.getAsJsonArray("prices")) {
            JsonObject entry = element.getAsJsonObject();
            prices.put(entry.get("type").getAsString(), entry.get("priceCents").getAsLong());
        }
        return new SpawnerPrices(json.get("updatedAt").getAsString(), json.get("source").getAsString(), Map.copyOf(prices));
    }

    public static String typeOf(ItemKey key) {
        String base = key.base();
        String path = base.substring(base.indexOf(':') + 1);
        return path.endsWith(SUFFIX) ? path.substring(0, path.length() - SUFFIX.length()) : null;
    }

    public OptionalLong unitCents(ItemKey key) {
        String type = typeOf(key);
        Long cents = type == null ? null : unitCentsByType.get(type);
        return cents == null ? OptionalLong.empty() : OptionalLong.of(cents);
    }

    public boolean isEmpty() {
        return unitCentsByType.isEmpty();
    }
}
