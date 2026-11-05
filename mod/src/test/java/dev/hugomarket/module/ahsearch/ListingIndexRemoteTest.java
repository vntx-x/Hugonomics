package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.parse.ItemKeyFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListingIndexRemoteTest {
    private static Listing listing(String seller, long priceCents, long expiresIn) {
        ItemView item = new ItemView("minecraft:diamond", 4, "Diamant", List.of(), Map.of(), Map.of(), null, null, List.of(), null, null, Map.of(), List.of());
        return new Listing(0, item, ItemKeyFactory.of(item), priceCents, seller, expiresIn, ListingStatus.ACTIVE, false);
    }

    @Test
    void remoteEntriesFillInWithoutDuplicatingLocalOnes() {
        ListingIndex index = new ListingIndex(15 * 60_000);
        index.addPage(List.of(listing("Seller", 1000, 3600)), 1_000_000);
        List<ListingIndex.RemoteEntry> remote = List.of(
                new ListingIndex.RemoteEntry("aaa", listing("", 1000, 3599), 990_000, 1_000_000 + 3599_000),
                new ListingIndex.RemoteEntry("bbb", listing("", 2000, 7200), 995_000, 1_000_000 + 7200_000),
                new ListingIndex.RemoteEntry("ccc", listing("", 3000, 10), 900_000, 999_000));
        index.replaceRemote(remote, 1_000_000);
        List<ListingIndex.Entry> all = index.query(SearchQuery.EMPTY);
        assertEquals(2, all.size());
        assertEquals(1, all.stream().filter(ListingIndex::isRemote).count());
        assertTrue(all.stream().anyMatch(e -> e.listing().priceCents() == 2000 && ListingIndex.isRemote(e)));
    }

    @Test
    void replacingRemoteDropsOldRemoteButKeepsLocal() {
        ListingIndex index = new ListingIndex(15 * 60_000);
        index.addPage(List.of(listing("Seller", 1000, 3600)), 1_000_000);
        index.replaceRemote(List.of(new ListingIndex.RemoteEntry("x", listing("", 5000, 3600), 1_000_000, 1_000_000 + 3600_000)), 1_000_000);
        index.replaceRemote(List.of(), 1_000_000);
        assertEquals(1, index.size());
        assertEquals("Seller", index.query(SearchQuery.EMPTY).get(0).listing().seller());
    }

    @Test
    void parsesEnchantmentsFromKeyVariant() {
        Map<String, Integer> enchants = dev.hugomarket.store.ListingsJson.enchantments("efficiency5;mending1;unbreaking3");
        assertEquals(3, enchants.size());
        assertEquals(5, enchants.get("efficiency"));
        assertTrue(dev.hugomarket.store.ListingsJson.enchantments("").isEmpty());
    }
}
