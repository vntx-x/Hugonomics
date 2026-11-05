package dev.hugomarket.store;

import dev.hugomarket.model.Order;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.parse.OrderListParser;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderBookTest {
    private final List<Order> orders = new OrderListParser(HugoGuiRules.load()).parse(DumpLoader.container("order-list/order-list-1.json"));

    @Test
    void bestOrderIsHighestUnitPriceWithSummedOpenUnits() {
        OrderBook book = new OrderBook();
        book.pageSeen(orders, 1_000);
        OrderBook.BestOrder best = book.best("elytra#mending1;unbreaking3");
        assertNotNull(best);
        assertEquals(172_000_000_000L, best.unitPriceCents());
        long open = orders.stream().filter(o -> o.key().toString().equals("elytra#mending1;unbreaking3")).mapToLong(Order::open).sum();
        assertEquals(open, best.openUnits());
        assertNull(book.best("dragon_egg"));
    }

    @Test
    void pruneDropsStaleKeys() {
        OrderBook book = new OrderBook();
        book.pageSeen(orders, 1_000);
        assertTrue(book.size() > 0);
        book.pageSeen(List.of(), 1_000 + 16 * 60_000);
        assertEquals(0, book.size());
    }

    @Test
    void flipMathBothDirections() {
        PriceStats ah = new PriceStats(100_000, 120_000, 5, 20, 0);
        OrderBook.BestOrder order = new OrderBook.BestOrder("k", 150_000, 10, "x", 0);
        FlipMath.Flip toOrder = FlipMath.ahToOrder(ah, order, 64);
        assertEquals(50_000, toOrder.marginUnitCents());
        assertEquals(10, toOrder.units());
        assertEquals(500_000, toOrder.totalMarginCents());
        assertEquals(50.0, toOrder.marginPercent(), 1e-9);

        Fees fees = Fees.load();
        FlipMath.Flip toAh = FlipMath.orderToAh(new OrderBook.BestOrder("k", 50_000, 10, "x", 0), ah, fees, 1);
        assertEquals(100_000 - fees.saleTax(100_000) - fees.listingFee(100_000), toAh.sellUnitCents());
        assertTrue(toAh.marginUnitCents() > 0);
        assertNull(FlipMath.ahToOrder(null, order, 1));
    }
}
