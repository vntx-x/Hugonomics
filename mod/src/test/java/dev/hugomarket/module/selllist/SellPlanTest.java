package dev.hugomarket.module.selllist;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.store.Fees;
import dev.hugomarket.store.PriceStats;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SellPlanTest {
    private static final Fees FEES = new Fees(1, List.of(new Fees.Tier(1_000_000, 1), new Fees.Tier(-1, 2)), 1);

    private static ItemView item(String id, int count) {
        return new ItemView("minecraft:" + id, count, id, List.of(), Map.of(), Map.of(), null, null, List.of(), null, null, Map.of(), List.of());
    }

    private static ItemKey key(ItemView item) {
        String id = item.id().substring("minecraft:".length());
        return id.equals("shulker_box") ? new ItemKey(id, ItemKey.FILLED, false) : new ItemKey(id, "", false);
    }

    private static PriceStats stats(ItemKey key) {
        return switch (key.base()) {
            case "diamond" -> new PriceStats(100_00, 120_00, 8, 8, 0);
            case "emerald" -> new PriceStats(20_00, 25_00, 4, 4, 0);
            case "shulker_box" -> new PriceStats(1_000_00, 1_000_00, 4, 4, 0);
            default -> null;
        };
    }

    @Test
    void groupsStacksUndercutsLowestAndSortsByValue() {
        List<ItemView> items = List.of(item("emerald", 64), item("diamond", 10), item("diamond", 5), item("dirt", 64), item("shulker_box", 1));
        List<SellPlan.Line> lines = SellPlan.build(items, SellPlanTest::key, SellPlanTest::stats, FEES);
        assertEquals(2, lines.size());
        SellPlan.Line diamonds = lines.get(0);
        assertEquals("diamond", diamonds.key().toString());
        assertEquals(15, diamonds.count());
        assertEquals(95_00, diamonds.unitCents());
        assertEquals(1_425_00, diamonds.totalCents());
        assertEquals(1_425_00 - 14_25 - 14_25, diamonds.netCents());
        assertEquals(64, lines.get(1).count());
        assertEquals(19_00, lines.get(1).unitCents());
        assertEquals(diamonds.netCents() + lines.get(1).netCents(), SellPlan.totalNet(lines));
    }
}
