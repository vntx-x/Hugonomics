package dev.hugomarket.module.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.hugomarket.model.Listing;

import java.time.Instant;
import java.util.List;

public final class ObservationBatch {
    private static final int MAX_LISTINGS = 60;
    private static final int MAX_NAME = 120;

    private ObservationBatch() {
    }

    public static JsonObject build(List<Listing> listings, String clientId, String clientVersion, String mcVersion,
                                   long observedAt, String pepper) {
        JsonObject batch = new JsonObject();
        batch.addProperty("clientId", clientId);
        batch.addProperty("clientVersion", clientVersion);
        batch.addProperty("mcVersion", mcVersion);
        batch.addProperty("observedAt", Instant.ofEpochMilli(observedAt).toString());
        batch.addProperty("source", "ah_list");
        JsonArray array = new JsonArray();
        for (Listing listing : listings) {
            if (!listing.isMarketListing() || array.size() >= MAX_LISTINGS) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("key", listing.key().toString());
            entry.addProperty("baseId", listing.item().id());
            String name = listing.item().displayName().isBlank() ? listing.key().toString() : listing.item().displayName().strip();
            entry.addProperty("displayName", name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name);
            entry.addProperty("count", listing.item().count());
            entry.addProperty("priceCents", listing.priceCents());
            entry.addProperty("sellerHash", SellerHash.of(listing.seller(), pepper));
            entry.addProperty("expiresInSeconds", listing.expiresInSeconds());
            entry.addProperty("slot", listing.slot());
            array.add(entry);
        }
        batch.add("listings", array);
        return batch;
    }
}
