package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Listing;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.UserData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public final class AhSearchState {
    private final MarketSession session;
    private final UserData userData;
    private final Map<String, ItemStack> icons = new HashMap<>();
    private SearchQuery query = SearchQuery.EMPTY;
    private List<ListingIndex.Entry> results = List.of();
    private List<Listing> currentPage = List.of();
    private final List<Runnable> changeListeners = new ArrayList<>();

    public AhSearchState(MarketSession session, UserData userData) {
        this.session = session;
        this.userData = userData;
    }

    public void onChange(Runnable listener) {
        changeListeners.add(listener);
    }

    public SearchQuery query() {
        return query;
    }

    public void setQuery(SearchQuery query) {
        this.query = query;
        requery();
    }

    public List<ListingIndex.Entry> results() {
        return results;
    }

    public List<Listing> currentPage() {
        return currentPage;
    }

    public int indexedCount() {
        return session.size();
    }

    public ItemStack icon(ListingIndex.Entry entry) {
        ItemStack stack = icons.get(entry.id());
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public boolean isWatched(Listing listing) {
        return userData.isWatched(listing.key().toString());
    }

    public void toggleWatch(Listing listing) {
        userData.toggleWatch(listing.key().toString());
        requery();
    }

    public void pageSeen(List<Listing> listings, List<ItemStack> stacks, long seenAt) {
        currentPage = listings;
        List<String> ids = session.addPage(listings, seenAt);
        for (int i = 0; i < ids.size(); i++) {
            icons.put(ids.get(i), stacks.get(i));
        }
        icons.keySet().removeIf(id -> session.index().get(id) == null);
        requery();
    }

    public void pageClosed() {
        currentPage = List.of();
        notifyChange();
    }

    public boolean matchesCurrentPage(Listing listing) {
        return query.matches(listing, userData::isWatched);
    }

    public OptionalDouble deal(Listing listing) {
        return Deals.ratio(session.stats(listing.key()), listing);
    }

    private void requery() {
        List<ListingIndex.Entry> found = session.index().query(query, userData::isWatched);
        if (query.sort() == SearchQuery.Sort.DEAL) {
            found = new ArrayList<>(found);
            found.sort(Comparator.comparingDouble(e -> deal(e.listing()).orElse(Double.MAX_VALUE)));
        }
        results = found;
        notifyChange();
    }

    private void notifyChange() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }
}
