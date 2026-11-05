package dev.hugomarket.store;

import dev.hugomarket.model.ItemKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceStoreTest {
    @Test
    void roundTripsThroughDisk(@TempDir Path dir) {
        Path file = dir.resolve("price-cache.json");
        PriceStore store = new PriceStore(file);
        store.put("nether_star", new PriceStats(80_000_000, 81_000_000, 12, 12, 1_000));
        store.saveIfDirty();
        assertTrue(store.savedAt() > 0);

        PriceStore loaded = new PriceStore(file);
        loaded.load();
        assertEquals(1, loaded.size());
        assertEquals(81_000_000, loaded.get("nether_star").medianUnitCents());
        assertNull(loaded.get("dragon_egg"));
    }

    @Test
    void sessionPrefersBackendUnlessSessionIsMuchNewer(@TempDir Path dir) {
        PriceStore backend = new PriceStore(dir.resolve("cache.json"));
        MarketSession session = new MarketSession(backend);
        ItemKey key = ItemKey.parse("emerald");
        assertNull(session.stats(key));
        assertEquals(1, session.drainWanted(10).size());

        backend.put("emerald", new PriceStats(100, 120, 5, 5, 5_000));
        PriceStats stats = session.stats(key);
        assertNotNull(stats);
        assertEquals(120, stats.medianUnitCents());
    }
}
