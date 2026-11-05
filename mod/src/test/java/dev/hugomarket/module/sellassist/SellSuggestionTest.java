package dev.hugomarket.module.sellassist;

import dev.hugomarket.store.PriceStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellSuggestionTest {
    private final PriceStats stats = new PriceStats(2_000_000, 2_500_000, 5, 40, 0);

    @Test
    void suggestionsScaleWithStackSize() {
        assertEquals(20_000, SellSuggestion.lowestStackDollars(stats, 1));
        assertEquals(1_280_000, SellSuggestion.lowestStackDollars(stats, 64));
        assertEquals(1_600_000, SellSuggestion.medianStackDollars(stats, 64));
        assertEquals(19_000, SellSuggestion.undercutStackDollars(stats, 1));
    }

    @Test
    void undercutNeverDropsBelowOneDollar() {
        PriceStats cheap = new PriceStats(100, 100, 3, 3, 0);
        assertEquals(1, SellSuggestion.undercutStackDollars(cheap, 1));
    }

    @Test
    void parsesTypedPrices() {
        assertEquals(10_000, SellSuggestion.parseTypedDollars("10000").getAsLong());
        assertEquals(1_250_000, SellSuggestion.parseTypedDollars("1.250.000").getAsLong());
        assertTrue(SellSuggestion.parseTypedDollars("").isEmpty());
        assertTrue(SellSuggestion.parseTypedDollars("12k").isEmpty());
    }

    @Test
    void warnsOnlyBeyondFactorTen() {
        assertEquals(SellSuggestion.Warning.NONE, SellSuggestion.check(stats, 1, 25_000));
        assertEquals(SellSuggestion.Warning.NONE, SellSuggestion.check(stats, 1, 2_600));
        assertEquals(SellSuggestion.Warning.FAR_BELOW, SellSuggestion.check(stats, 1, 2_400));
        assertEquals(SellSuggestion.Warning.FAR_ABOVE, SellSuggestion.check(stats, 1, 260_000));
        assertEquals(SellSuggestion.Warning.NONE, SellSuggestion.check(null, 1, 1));
        assertEquals(SellSuggestion.Warning.NONE, SellSuggestion.check(new PriceStats(100, 100, 2, 2, 0), 1, 1_000_000));
    }
}
