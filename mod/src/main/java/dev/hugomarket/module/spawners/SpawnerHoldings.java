package dev.hugomarket.module.spawners;

import dev.hugomarket.model.ItemView;
import dev.hugomarket.parse.ItemKeyFactory;
import dev.hugomarket.store.SpawnerPrices;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SpawnerHoldings {
    private static final String SPAWNER_ID = "minecraft:spawner";

    private SpawnerHoldings() {
    }

    static Map<String, Integer> count(List<ItemView> items) {
        Map<String, Integer> out = new HashMap<>();
        for (ItemView item : items) {
            add(out, item);
        }
        return out;
    }

    private static void add(Map<String, Integer> out, ItemView item) {
        if (SPAWNER_ID.equals(item.id())) {
            String type = SpawnerPrices.typeOf(ItemKeyFactory.of(item));
            if (type != null) {
                out.merge(type, item.count(), Integer::sum);
            }
        }
        for (ItemView inner : item.contents()) {
            add(out, inner);
        }
    }
}
