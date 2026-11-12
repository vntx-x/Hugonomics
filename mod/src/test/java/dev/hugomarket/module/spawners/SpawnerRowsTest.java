package dev.hugomarket.module.spawners;

import dev.hugomarket.store.SpawnerPrices;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpawnerRowsTest {
    @Test
    void sortsMostValuableSpawnerFirst() {
        SpawnerPrices prices = new SpawnerPrices("", "", Map.of("zombie", 10_00L, "iron_golem", 900_00L, "skeleton", 10_00L));
        List<String> types = SpawnerRows.build(prices).stream().map(SpawnerRows.Row::type).toList();
        assertEquals(List.of("iron_golem", "skeleton", "zombie"), types);
    }
}
