package dev.hugomarket.store;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlipTest {
    private static final Fees FEES = new Fees(1, List.of(new Fees.Tier(1_000_000, 1), new Fees.Tier(-1, 2)), 1);

    @Test
    void ahFlipSubtractsSaleTax() {
        PriceStats stats = new PriceStats(90_00, 100_00, 5, 5, 0);
        assertEquals(10 * 100_00 - 10 * 100_00 / 100 - 800_00, Flip.viaAh(800_00, 10, stats, false, FEES).getAsLong());
        assertTrue(Flip.viaAh(800_00, 10, stats, false, FEES).getAsLong() > 0);
        assertTrue(Flip.viaAh(2_000_00, 10, stats, false, FEES).getAsLong() < 0);
    }

    @Test
    void ahFlipNeedsEnoughListingsAndNoFilledContainers() {
        assertTrue(Flip.viaAh(1, 1, new PriceStats(1, 1, 2, 2, 0), false, FEES).isEmpty());
        assertTrue(Flip.viaAh(1, 1, new PriceStats(1, 100, 9, 9, 0), true, FEES).isEmpty());
        assertTrue(Flip.viaAh(1, 1, null, false, FEES).isEmpty());
    }

    @Test
    void orderFlipNeedsRoomForTheWholeStack() {
        OrderBook.BestOrder order = new OrderBook.BestOrder("diamond", 120_00, 64, "x", 0);
        assertEquals(64 * 120_00 - 6_000_00, Flip.viaOrder(6_000_00, 64, order).getAsLong());
        assertTrue(Flip.viaOrder(1, 65, order).isEmpty());
        assertTrue(Flip.viaOrder(1, 1, null).isEmpty());
    }
}
