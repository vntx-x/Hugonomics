package dev.hugomarket.store;

import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.module.ahsearch.SearchQuery;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDataTest {
    @Test
    void watchlistAndPricesSurviveReload(@TempDir Path dir) {
        UserData data = new UserData(dir.resolve("templates.json"));
        assertTrue(data.toggleWatch("nether_star"));
        data.rememberUnitPrice("nether_star", 80_000_000);
        data.rememberUnitPrice("nether_star", 79_000_000);

        UserData reloaded = new UserData(dir.resolve("templates.json"));
        reloaded.load();
        assertTrue(reloaded.isWatched("nether_star"));
        assertEquals(79_000_000, reloaded.lastUnitPrice("nether_star").getAsLong());
        assertTrue(reloaded.lastUnitPrice("emerald").isEmpty());
        assertFalse(reloaded.toggleWatch("nether_star"));
    }

    @Test
    void watchedFilterRestrictsResults(@TempDir Path dir) {
        UserData data = new UserData(dir.resolve("templates.json"));
        data.toggleWatch("nether_star");
        ListingIndex index = new ListingIndex(60_000);
        index.addPage(new AhListingParser(HugoGuiRules.load()).parse(DumpLoader.container("ah-list/ah-list-2.json")).listings(), 1_000);
        var watched = index.query(SearchQuery.EMPTY.toggleWatched(), data::isWatched);
        assertTrue(watched.size() >= 20);
        assertTrue(watched.stream().allMatch(e -> e.listing().key().toString().equals("nether_star")));
        assertEquals(0, index.query(SearchQuery.EMPTY.toggleWatched()).size());
    }
}
