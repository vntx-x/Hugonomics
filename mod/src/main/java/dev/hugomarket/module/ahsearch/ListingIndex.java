package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Listing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class ListingIndex {
    public record Entry(String id, Listing listing, long firstSeen, long lastSeen, long expiresAt) {
    }

    private static final long[] SECOND_TOLERANCE = {0, -1, 1};
    private static final int MAX_DUPLICATES = 64;

    private final long maxAgeMillis;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public ListingIndex(long maxAgeMillis) {
        this.maxAgeMillis = maxAgeMillis;
    }

    public List<String> addPage(List<Listing> listings, long seenAt) {
        List<String> ids = new ArrayList<>(listings.size());
        Set<String> usedThisPage = new HashSet<>();
        for (Listing listing : listings) {
            long expiresAt = listing.expiresInSeconds() < 0 ? 0 : seenAt + listing.expiresInSeconds() * 1000;
            String id = resolveId(listing, expiresAt, usedThisPage);
            Entry previous = entries.get(id);
            long firstSeen = previous == null ? seenAt : previous.firstSeen();
            long stableExpiry = previous == null ? expiresAt : previous.expiresAt();
            entries.put(id, new Entry(id, listing, firstSeen, seenAt, stableExpiry));
            ids.add(id);
        }
        prune(seenAt);
        return ids;
    }

    private String resolveId(Listing listing, long expiresAt, Set<String> usedThisPage) {
        String base = listing.seller() + "|" + listing.key() + "|" + listing.item().count() + "|" + listing.priceCents() + "|";
        long second = expiresAt / 1000;
        for (long delta : SECOND_TOLERANCE) {
            String prefix = base + (second + delta);
            for (int n = 1; n <= MAX_DUPLICATES; n++) {
                String candidate = n == 1 ? prefix : prefix + "#" + n;
                if (!entries.containsKey(candidate)) {
                    break;
                }
                if (usedThisPage.add(candidate)) {
                    return candidate;
                }
            }
        }
        String prefix = base + second;
        for (int n = 1; ; n++) {
            String candidate = n == 1 ? prefix : prefix + "#" + n;
            if (!entries.containsKey(candidate) && usedThisPage.add(candidate)) {
                return candidate;
            }
        }
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    public Entry get(String id) {
        return entries.get(id);
    }

    public List<Entry> query(SearchQuery query) {
        return query(query, SearchQuery.NOTHING_WATCHED);
    }

    public List<Entry> query(SearchQuery query, Predicate<String> isWatched) {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : entries.values()) {
            if (query.matches(entry.listing(), isWatched)) {
                result.add(entry);
            }
        }
        result.sort(comparator(query.sort()));
        return result;
    }

    private static Comparator<Entry> comparator(SearchQuery.Sort sort) {
        Comparator<Entry> unit = Comparator.comparingLong(e -> e.listing().unitPriceCents());
        Comparator<Entry> expiry = Comparator.comparingLong(Entry::expiresAt);
        return switch (sort) {
            case UNIT_ASC -> unit.thenComparing(expiry);
            case UNIT_DESC -> unit.reversed().thenComparing(expiry);
            case EXPIRES_ASC -> expiry.thenComparing(unit);
            case NEWEST -> expiry.reversed().thenComparing(unit);
        };
    }

    private void prune(long now) {
        Iterator<Entry> it = entries.values().iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            boolean expired = entry.expiresAt() > 0 && entry.expiresAt() <= now;
            if (expired || now - entry.lastSeen() > maxAgeMillis) {
                it.remove();
            }
        }
    }
}
