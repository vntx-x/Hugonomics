package dev.hugomarket.store;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.SlotView;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmSessionTest {
    private final MarketSession market = new MarketSession(new PriceStore(Path.of("build", "unused-cache.json")));

    @Test
    void removedItemsAreDetectedBetweenSnapshots() {
        ContainerView before = DumpLoader.container("chest/chest-1.json");
        List<SlotView> slots = new ArrayList<>(before.slots());
        slots.set(0, new SlotView(0, false, null));
        ContainerView after = new ContainerView(before.title(), before.menuType(), before.containerSlots(), slots);
        Map<String, Integer> removed = ContainerCounts.removed(ContainerCounts.of(before), ContainerCounts.of(after));
        assertEquals(Map.of("gravel", 64), removed);
        assertTrue(ContainerCounts.removed(ContainerCounts.of(after), ContainerCounts.of(before)).isEmpty());
    }

    @Test
    void sessionAggregatesCollectedItemsAndIncome() {
        long start = 1_000_000;
        FarmSession session = new FarmSession(start);
        session.collected(Map.of("gravel", 64, "flint", 10), start + 60_000);
        session.collected(Map.of("gravel", 64), start + 120_000);
        session.income(50_000, start + 180_000);
        FarmSession.Summary summary = session.summary(market, start + 180_000);
        assertEquals(138, summary.itemsCollected());
        assertEquals(50_000, summary.incomeCents());
        assertEquals(180_000, summary.durationMs());
        assertEquals(1_000_000, summary.incomePerHourCents());
        assertEquals("gravel", summary.top().get(0).key());
        assertEquals(128, summary.top().get(0).count());
    }

    @Test
    void idleGapsAreCappedAndResetClears() {
        long start = 1_000_000;
        FarmSession session = new FarmSession(start);
        session.collected(Map.of("gravel", 1), start + 60_000);
        session.collected(Map.of("gravel", 1), start + 60_000 + 60 * 60_000);
        assertEquals(60_000 + 10 * 60_000, session.summary(market, start + 60_000 + 60 * 60_000).durationMs());
        session.reset(start);
        assertEquals(0, session.summary(market, start).itemsCollected());
    }
}
