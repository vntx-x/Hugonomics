package dev.hugomarket.store;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.module.ahsearch.ListingIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ListingsJson {
    private static final Pattern ENCHANT = Pattern.compile("^([a-z_]+?)(\\d+)$");

    private ListingsJson() {
    }

    public static List<ListingIndex.RemoteEntry> parse(JsonObject response, long now) {
        List<ListingIndex.RemoteEntry> out = new ArrayList<>();
        JsonArray array = response.getAsJsonArray("listings");
        if (array == null) {
            return out;
        }
        for (JsonElement element : array) {
            JsonObject json = element.getAsJsonObject();
            ItemKey key = ItemKey.parse(json.get("key").getAsString());
            int count = Math.max(1, json.get("count").getAsInt());
            ItemView item = new ItemView(json.get("baseId").getAsString(), count, json.get("displayName").getAsString(), List.of(),
                    enchantments(key.variant()), Map.of(), null, null, List.of(), null, null, Map.of(), List.of());
            long expiresAt = json.get("expiresAt").getAsLong();
            long expiresIn = expiresAt <= 0 ? -1 : Math.max(0, (expiresAt - now) / 1000);
            Listing listing = new Listing(-1, item, key, json.get("priceCents").getAsLong(), "", expiresIn, ListingStatus.ACTIVE, false);
            out.add(new ListingIndex.RemoteEntry(json.get("id").getAsString(), listing, json.get("lastSeen").getAsLong(), expiresAt));
        }
        return out;
    }

    public static Map<String, Integer> enchantments(String variant) {
        if (variant.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> out = new TreeMap<>();
        for (String part : variant.split(";")) {
            Matcher m = ENCHANT.matcher(part);
            if (m.matches()) {
                out.put(m.group(1), Integer.parseInt(m.group(2)));
            }
        }
        return Map.copyOf(out);
    }
}
