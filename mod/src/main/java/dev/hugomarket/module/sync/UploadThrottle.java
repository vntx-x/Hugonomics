package dev.hugomarket.module.sync;

import dev.hugomarket.model.Listing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class UploadThrottle {
    private static final long BUCKET_MS = 10_000;
    private static final int MAX_ENTRIES = 4000;

    private final long minGapMs;
    private final Map<String, Long> lastSent = new HashMap<>();

    public UploadThrottle(long minGapMs) {
        this.minGapMs = minGapMs;
    }

    public List<Listing> filter(List<Listing> listings, long observedAt) {
        List<Listing> due = new ArrayList<>();
        for (Listing listing : listings) {
            if (!listing.isMarketListing()) {
                continue;
            }
            long bucket = (observedAt + listing.expiresInSeconds() * 1000L) / BUCKET_MS;
            String prefix = listing.seller() + "|" + listing.key() + "|" + listing.item().count() + "|" + listing.priceCents() + "|";
            if (recentlySent(prefix, bucket, observedAt)) {
                continue;
            }
            lastSent.put(prefix + bucket, observedAt);
            due.add(listing);
        }
        if (lastSent.size() > MAX_ENTRIES) {
            prune(observedAt);
        }
        return due;
    }

    private boolean recentlySent(String prefix, long bucket, long now) {
        for (long b = bucket - 1; b <= bucket + 1; b++) {
            Long at = lastSent.get(prefix + b);
            if (at != null && now - at < minGapMs) {
                return true;
            }
        }
        return false;
    }

    private void prune(long now) {
        Iterator<Map.Entry<String, Long>> it = lastSent.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() >= minGapMs) {
                it.remove();
            }
        }
    }
}
