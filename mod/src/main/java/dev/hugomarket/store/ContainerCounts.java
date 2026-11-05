package dev.hugomarket.store;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.SlotView;
import dev.hugomarket.parse.ItemKeyFactory;

import java.util.HashMap;
import java.util.Map;

public final class ContainerCounts {
    private ContainerCounts() {
    }

    public static Map<String, Integer> of(ContainerView view) {
        Map<String, Integer> counts = new HashMap<>();
        for (SlotView slot : view.slots()) {
            if (slot.playerInventory() || slot.isEmpty()) {
                continue;
            }
            counts.merge(ItemKeyFactory.of(slot.item()).toString(), slot.item().count(), Integer::sum);
        }
        return counts;
    }

    public static Map<String, Integer> added(Map<String, Integer> before, Map<String, Integer> after) {
        return removed(after, before);
    }

    public static Map<String, Integer> removed(Map<String, Integer> before, Map<String, Integer> after) {
        Map<String, Integer> removed = new HashMap<>();
        for (Map.Entry<String, Integer> entry : before.entrySet()) {
            int diff = entry.getValue() - after.getOrDefault(entry.getKey(), 0);
            if (diff > 0) {
                removed.put(entry.getKey(), diff);
            }
        }
        return removed;
    }
}
