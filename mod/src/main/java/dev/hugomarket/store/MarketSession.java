package dev.hugomarket.store;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.Listing;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.module.ahsearch.SearchQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MarketSession {
    private static final long MAX_AGE_MS = 15 * 60_000;

    private final ListingIndex index = new ListingIndex(MAX_AGE_MS);
    private final Map<String, PriceStats> statsByKey = new HashMap<>();
    private final List<Runnable> changeListeners = new ArrayList<>();
    private boolean statsDirty;

    public void onChange(Runnable listener) {
        changeListeners.add(listener);
    }

    public ListingIndex index() {
        return index;
    }

    public List<String> addPage(List<Listing> listings, long seenAt) {
        List<String> ids = index.addPage(listings, seenAt);
        statsDirty = true;
        for (Runnable listener : changeListeners) {
            listener.run();
        }
        return ids;
    }

    public PriceStats stats(ItemKey key) {
        if (statsDirty) {
            rebuildStats();
        }
        return statsByKey.get(key.toString());
    }

    public int size() {
        return index.size();
    }

    private void rebuildStats() {
        statsDirty = false;
        statsByKey.clear();
        Map<String, List<ListingIndex.Entry>> byKey = new HashMap<>();
        for (ListingIndex.Entry entry : index.query(SearchQuery.EMPTY)) {
            byKey.computeIfAbsent(entry.listing().key().toString(), k -> new ArrayList<>()).add(entry);
        }
        for (Map.Entry<String, List<ListingIndex.Entry>> group : byKey.entrySet()) {
            List<ListingIndex.Entry> entries = group.getValue();
            long[] units = new long[entries.size()];
            int[] counts = new int[entries.size()];
            long lastSeen = 0;
            for (int i = 0; i < entries.size(); i++) {
                units[i] = entries.get(i).listing().unitPriceCents();
                counts[i] = entries.get(i).listing().item().count();
                lastSeen = Math.max(lastSeen, entries.get(i).lastSeen());
            }
            statsByKey.put(group.getKey(), PriceStats.of(units, counts, lastSeen));
        }
    }
}
