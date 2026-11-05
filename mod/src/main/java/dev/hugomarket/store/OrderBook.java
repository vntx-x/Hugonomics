package dev.hugomarket.store;

import dev.hugomarket.model.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class OrderBook {
    public record BestOrder(String key, long unitPriceCents, long openUnits, String owner, long lastSeen) {
    }

    private static final long MAX_AGE_MS = 15 * 60_000;

    private final Map<String, List<Order>> byKey = new HashMap<>();
    private final Map<String, Long> seenAt = new HashMap<>();

    public void pageSeen(List<Order> orders, long now) {
        Map<String, List<Order>> fresh = new HashMap<>();
        for (Order order : orders) {
            if (order.open() <= 0) {
                continue;
            }
            fresh.computeIfAbsent(order.key().toString(), k -> new ArrayList<>()).add(order);
        }
        for (Map.Entry<String, List<Order>> entry : fresh.entrySet()) {
            byKey.put(entry.getKey(), List.copyOf(entry.getValue()));
            seenAt.put(entry.getKey(), now);
        }
        prune(now);
    }

    public BestOrder best(String key) {
        List<Order> orders = byKey.get(key);
        if (orders == null || orders.isEmpty()) {
            return null;
        }
        Order best = orders.get(0);
        long open = 0;
        for (Order order : orders) {
            open += order.open();
            if (order.unitPriceCents() > best.unitPriceCents()) {
                best = order;
            }
        }
        return new BestOrder(key, best.unitPriceCents(), open, best.owner(), seenAt.getOrDefault(key, 0L));
    }

    public int size() {
        return byKey.size();
    }

    private void prune(long now) {
        Iterator<Map.Entry<String, Long>> it = seenAt.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() > MAX_AGE_MS) {
                byKey.remove(entry.getKey());
                it.remove();
            }
        }
    }
}
