package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Listing;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListingIndexTest {
    private final AhListingParser parser = new AhListingParser(HugoGuiRules.load());
    private final List<Listing> pageOne = parser.parse(DumpLoader.container("ah-list/ah-list-1.json")).listings();
    private final List<Listing> pageTwo = parser.parse(DumpLoader.container("ah-list/ah-list-2.json")).listings();

    @Test
    void sameListingSeenTwiceIsStoredOnce() {
        ListingIndex index = new ListingIndex(60_000);
        List<String> first = index.addPage(pageOne, 1_000);
        int size = index.size();
        List<String> second = index.addPage(pageOne, 1_700);
        assertEquals(size, index.size());
        assertEquals(pageOne.size(), size);
        assertEquals(first, second);
        assertTrue(size >= 30);
    }

    @Test
    void identicalListingsFromOneSellerStaySeparate() {
        ListingIndex index = new ListingIndex(60_000);
        index.addPage(pageTwo, 1_000);
        long stars = pageTwo.stream().filter(l -> l.item().id().equals("minecraft:nether_star")).count();
        long found = index.query(SearchQuery.EMPTY.withText("nether star")).stream()
                .filter(e -> e.listing().item().id().equals("minecraft:nether_star")).count();
        assertEquals(stars, found);
        assertTrue(stars >= 20);
    }

    @Test
    void twoPagesAccumulate() {
        ListingIndex index = new ListingIndex(60_000);
        index.addPage(pageOne, 1_000);
        index.addPage(pageTwo, 7_000);
        assertTrue(index.size() > pageOne.size());
        assertTrue(index.size() <= pageOne.size() + pageTwo.size());
    }

    @Test
    void textQueryMatchesNameKeyOrSeller() {
        ListingIndex index = new ListingIndex(60_000);
        index.addPage(pageTwo, 1_000);
        List<ListingIndex.Entry> stars = index.query(SearchQuery.EMPTY.withText("nether star"));
        assertTrue(stars.size() >= 5);
        for (ListingIndex.Entry entry : stars) {
            String haystack = (entry.listing().item().displayName() + " " + entry.listing().key()).toLowerCase(java.util.Locale.ROOT);
            assertTrue(haystack.contains("nether") && haystack.contains("star"));
        }
        assertTrue(index.query(SearchQuery.EMPTY.withText("NETHER_STAR")).size() >= 20);
        assertTrue(index.query(SearchQuery.EMPTY.withText("player_9")).size() >= 20);
        assertEquals(0, index.query(SearchQuery.EMPTY.withText("gibtsnicht")).size());
    }

    @Test
    void sortsByUnitPrice() {
        ListingIndex index = new ListingIndex(60_000);
        index.addPage(pageOne, 1_000);
        List<ListingIndex.Entry> asc = index.query(SearchQuery.EMPTY.withSort(SearchQuery.Sort.UNIT_ASC));
        for (int i = 1; i < asc.size(); i++) {
            assertTrue(asc.get(i - 1).listing().unitPriceCents() <= asc.get(i).listing().unitPriceCents());
        }
        List<ListingIndex.Entry> desc = index.query(SearchQuery.EMPTY.withSort(SearchQuery.Sort.UNIT_DESC));
        assertEquals(asc.get(0).listing().unitPriceCents(), desc.get(desc.size() - 1).listing().unitPriceCents());
        assertEquals(asc.get(asc.size() - 1).listing().unitPriceCents(), desc.get(0).listing().unitPriceCents());
    }

    @Test
    void prunesStaleEntries() {
        ListingIndex index = new ListingIndex(10_000);
        index.addPage(pageOne, 1_000);
        index.addPage(List.of(), 20_000);
        assertEquals(0, index.size());
    }

    @Test
    void enchantedFilterKeepsOnlyEnchantedItems() {
        ListingIndex index = new ListingIndex(60_000);
        index.addPage(pageOne, 1_000);
        index.addPage(pageTwo, 1_000);
        for (ListingIndex.Entry entry : index.query(SearchQuery.EMPTY.toggleEnchanted())) {
            assertTrue(!entry.listing().item().enchantments().isEmpty() || !entry.listing().item().storedEnchantments().isEmpty());
        }
    }
}
