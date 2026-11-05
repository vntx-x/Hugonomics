package dev.hugomarket.module.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hugomarket.store.PriceStats;

import java.util.ArrayList;
import java.util.List;

public final class PriceJson {
    public record ItemSummary(String key, String baseId, String displayName, int activeListings, long lastSeen) {
    }

    public record HistoryPoint(long ts, long lowestUnitCents, long medianUnitCents, int listings) {
    }

    private PriceJson() {
    }

    public static PriceStats stats(JsonObject p) {
        return new PriceStats(p.get("lowestUnitCents").getAsLong(), p.get("medianUnitCents").getAsLong(),
                p.get("listings").getAsInt(), p.get("units").getAsInt(), p.get("lastSeen").getAsLong());
    }

    public static List<ItemSummary> items(JsonObject response) {
        List<ItemSummary> out = new ArrayList<>();
        JsonArray array = response.getAsJsonArray("items");
        if (array == null) {
            return out;
        }
        for (JsonElement element : array) {
            JsonObject item = element.getAsJsonObject();
            out.add(new ItemSummary(item.get("key").getAsString(), item.get("baseId").getAsString(),
                    item.get("displayName").getAsString(), item.get("activeListings").getAsInt(), item.get("lastSeen").getAsLong()));
        }
        return out;
    }

    public static List<HistoryPoint> history(JsonObject response) {
        List<HistoryPoint> out = new ArrayList<>();
        JsonArray array = response.getAsJsonArray("points");
        if (array == null) {
            return out;
        }
        for (JsonElement element : array) {
            JsonObject point = element.getAsJsonObject();
            out.add(new HistoryPoint(point.get("bucketTs").getAsLong(), point.get("lowestUnitCents").getAsLong(),
                    point.get("medianUnitCents").getAsLong(), point.get("listings").getAsInt()));
        }
        return out;
    }
}
