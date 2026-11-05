package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Listing;
import dev.hugomarket.store.MarketSession;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AhSearchState {
    private final MarketSession session;
    private final Map<String, ItemStack> icons = new HashMap<>();
    private SearchQuery query = SearchQuery.EMPTY;
    private List<ListingIndex.Entry> results = List.of();
    private List<Listing> currentPage = List.of();
    private final List<Runnable> changeListeners = new ArrayList<>();

    public AhSearchState(MarketSession session) {
        this.session = session;
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
        return query.matches(listing);
    }

    private void requery() {
        results = session.index().query(query);
        notifyChange();
    }

    private void notifyChange() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }
}
