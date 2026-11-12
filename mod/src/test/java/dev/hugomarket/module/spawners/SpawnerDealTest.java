package dev.hugomarket.module.spawners;

import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.SlotView;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpawnerDealTest {
    @Test
    void parsesTheWaysPlayersWritePrices() {
        assertEquals(OptionalLong.of(27_500_000_00L), SpawnerDeal.parsePriceCents("27,5m"));
        assertEquals(OptionalLong.of(27_500_000_00L), SpawnerDeal.parsePriceCents("27.5M"));
        assertEquals(OptionalLong.of(27_500_000_00L), SpawnerDeal.parsePriceCents("$27.500.000"));
        assertEquals(OptionalLong.of(27_500_000_00L), SpawnerDeal.parsePriceCents("27500000"));
        assertEquals(OptionalLong.of(27_500_000_00L), SpawnerDeal.parsePriceCents("27,5 Mio"));
        assertEquals(OptionalLong.of(1_200_000_000_00L), SpawnerDeal.parsePriceCents("1,2b"));
        assertEquals(OptionalLong.of(800_000_00L), SpawnerDeal.parsePriceCents("800k"));
        assertEquals(OptionalLong.empty(), SpawnerDeal.parsePriceCents("abc"));
        assertEquals(OptionalLong.empty(), SpawnerDeal.parsePriceCents("0"));
        assertEquals(OptionalLong.empty(), SpawnerDeal.parsePriceCents(""));
    }

    @Test
    void parsesCounts() {
        assertEquals(OptionalLong.of(8), SpawnerDeal.parseCount(" 8 "));
        assertEquals(OptionalLong.empty(), SpawnerDeal.parseCount("0"));
        assertEquals(OptionalLong.empty(), SpawnerDeal.parseCount("x8"));
    }

    @Test
    void comparesTheDealWithTheReferencePrice() {
        SpawnerDeal deal = new SpawnerDeal(8, 27_500_000_00L, 27_600_000_00L);
        assertEquals(220_000_000_00L, deal.totalCents());
        assertEquals(220_800_000_00L, deal.referenceTotalCents());
        assertEquals(27.5 / 27.6, deal.ratio().getAsDouble(), 1e-9);
        assertEquals(true, new SpawnerDeal(1, 5_00, 0).ratio().isEmpty());
    }

    @Test
    void countsOwnedSpawnersIncludingStacks() {
        List<ItemView> items = DumpLoader.container("enderchest/enderchest-1.json").containerItems().map(SlotView::item).toList();
        assertEquals(Map.of("skeleton", 9), SpawnerHoldings.count(items));
    }
}
