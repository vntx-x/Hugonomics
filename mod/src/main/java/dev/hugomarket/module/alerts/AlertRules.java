package dev.hugomarket.module.alerts;

import dev.hugomarket.model.Listing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AlertRules {
    public record Hit(Listing listing, long alertUnitCents) {
    }

    private static final long LISTING_COOLDOWN_MS = 10 * 60_000;

    private final Map<String, Long> notified = new HashMap<>();

    public List<Hit> check(List<Listing> listings, List<String> listingIds, Map<String, Long> alerts, long now) {
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < listings.size(); i++) {
            Listing listing = listings.get(i);
            Long alert = alerts.get(listing.key().toString());
            if (alert == null || !listing.isMarketListing() || listing.unitPriceCents() > alert) {
                continue;
            }
            String id = listingIds.get(i);
            Long last = notified.get(id);
            if (last != null && now - last < LISTING_COOLDOWN_MS) {
                continue;
            }
            notified.put(id, now);
            hits.add(new Hit(listing, alert));
        }
        notified.values().removeIf(ts -> now - ts > LISTING_COOLDOWN_MS * 6);
        return hits;
    }
}
