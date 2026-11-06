package dev.hugomarket.store;

import dev.hugomarket.module.ahsearch.ListingIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RemoteListingsCache {
    private final long maxAgeMs;
    private final Map<String, ListingIndex.RemoteEntry> byId = new HashMap<>();

    public RemoteListingsCache(long maxAgeMs) {
        this.maxAgeMs = maxAgeMs;
    }

    public void replace(List<ListingIndex.RemoteEntry> entries) {
        byId.clear();
        merge(entries);
    }

    public void merge(List<ListingIndex.RemoteEntry> entries) {
        for (ListingIndex.RemoteEntry entry : entries) {
            byId.put(entry.id(), entry);
        }
    }

    public List<ListingIndex.RemoteEntry> active(long now) {
        byId.values().removeIf(e -> (e.expiresAt() > 0 && e.expiresAt() <= now) || now - e.lastSeen() > maxAgeMs);
        return new ArrayList<>(byId.values());
    }

    public int size() {
        return byId.size();
    }
}
