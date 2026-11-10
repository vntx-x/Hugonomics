package dev.hugomarket.module.watchalerts;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.module.ahsearch.Deals;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.store.PriceStats;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Function;

public final class WatchBargains {
    public record Hit(ListingIndex.Entry entry, double ratio) {
    }

    private static final int MAX_SEEN = 4096;

    private final SequencedSet<String> seen = new LinkedHashSet<>();
    private boolean primed;

    public List<Hit> check(List<ListingIndex.Entry> entries, Set<String> watchlist, Function<ItemKey, PriceStats> stats) {
        List<Hit> hits = new ArrayList<>();
        for (ListingIndex.Entry entry : entries) {
            if (!watchlist.contains(entry.listing().key().toString()) || seen.contains(entry.id())) {
                continue;
            }
            OptionalDouble ratio = Deals.ratio(stats.apply(entry.listing().key()), entry.listing());
            if (ratio.isEmpty() || !Deals.isBargain(ratio.getAsDouble())) {
                continue;
            }
            if (seen.size() >= MAX_SEEN) {
                seen.removeFirst();
            }
            seen.add(entry.id());
            if (primed) {
                hits.add(new Hit(entry, ratio.getAsDouble()));
            }
        }
        if (!entries.isEmpty()) {
            primed = true;
        }
        return hits;
    }
}
