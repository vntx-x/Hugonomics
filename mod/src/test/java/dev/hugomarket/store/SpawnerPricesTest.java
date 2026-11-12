package dev.hugomarket.store;

import com.google.gson.JsonParser;
import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.SlotView;
import dev.hugomarket.parse.ItemKeyFactory;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnerPricesTest {
    private static SpawnerPrices prices() {
        return SpawnerPrices.parse(JsonParser.parseString("""
                {"updatedAt": "2026-11-12", "source": "test",
                 "prices": [{"type": "skeleton", "priceCents": 50000000}, {"type": "iron_golem", "priceCents": 900000000}]}
                """).getAsJsonObject());
    }

    @Test
    void readsTheSpawnerTypeFromHugoSpawnerKeys() {
        ContainerView enderchest = DumpLoader.container("enderchest/enderchest-1.json");
        ItemKey key = enderchest.containerItems().map(SlotView::item)
                .filter(item -> item.id().equals("minecraft:spawner")).map(ItemKeyFactory::of).findFirst().orElseThrow();
        assertEquals("skeleton", SpawnerPrices.typeOf(key));
        assertEquals(50_000_000, prices().unitCents(key).getAsLong());
        assertNull(SpawnerPrices.typeOf(ItemKey.parse("spawner")));
        assertTrue(prices().unitCents(ItemKey.parse("diamond")).isEmpty());
    }

    @Test
    void stackedSpawnersAddEverySpawnerToTheContainerValue() {
        ContainerView enderchest = DumpLoader.container("enderchest/enderchest-1.json");
        MarketSession market = new MarketSession(new PriceStore(Path.of("build", "unused-cache.json")));
        market.setSpawnerPrices(prices());
        ContainerValuation value = ContainerValuation.of(enderchest, market);
        assertEquals(9 * 50_000_000L, value.ahLowestCents());
        assertEquals(2, value.valuedByAh());
    }
}
