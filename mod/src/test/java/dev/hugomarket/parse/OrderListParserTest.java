package dev.hugomarket.parse;

import dev.hugomarket.model.Order;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderListParserTest {
    private final OrderListParser parser = new OrderListParser(HugoGuiRules.load());

    @Test
    void parsesOrdersWithEnchantedKeys() {
        List<Order> orders = parser.parse(DumpLoader.container("order-list/order-list-1.json"));
        assertTrue(orders.size() >= 30);

        Order first = orders.get(0);
        assertEquals(0, first.slot());
        assertEquals("minecraft:elytra", first.item().id());
        assertEquals("elytra#mending1;unbreaking3", first.key().toString());
        assertEquals(172_000_000_000L, first.unitPriceCents());
        assertEquals(0, first.delivered());
        assertEquals(1, first.total());
        assertEquals(1, first.open());
        assertEquals(0, first.paidOutCents());
        assertEquals(172_000_000_000L, first.paidTotalCents());
        assertTrue(first.owner().startsWith("player_"));
    }

    @Test
    void plainOrderHasNoVariant() {
        List<Order> orders = parser.parse(DumpLoader.container("order-list/order-list-1.json"));
        Order plain = orders.stream().filter(o -> o.key().variant().isEmpty()).findFirst().orElseThrow();
        assertEquals("elytra", plain.key().toString());
        assertEquals(4, plain.total());
    }
}
