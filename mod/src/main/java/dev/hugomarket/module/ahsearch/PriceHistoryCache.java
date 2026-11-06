package dev.hugomarket.module.ahsearch;

import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.module.sync.PriceJson;
import dev.hugomarket.net.BackendClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class PriceHistoryCache {
    private static final long TTL_MS = 5 * 60_000;
    private static final int MAX_ENTRIES = 64;
    private static final String RANGE = "7d";

    private record Cached(List<PriceJson.HistoryPoint> points, long loadedAt) {
    }

    private final BackendClient client;
    private final ClientScheduler scheduler;
    private final Map<String, Cached> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public PriceHistoryCache(BackendClient client, ClientScheduler scheduler) {
        this.client = client;
        this.scheduler = scheduler;
    }

    public void load(String key, Consumer<List<PriceJson.HistoryPoint>> onLoaded) {
        long now = System.currentTimeMillis();
        Cached cached = cache.get(key);
        if (cached != null && now - cached.loadedAt() < TTL_MS) {
            onLoaded.accept(cached.points());
            return;
        }
        if (client == null || !client.available()) {
            onLoaded.accept(List.of());
            return;
        }
        String path = "/v1/items/" + URLEncoder.encode(key, StandardCharsets.UTF_8) + "/history?range=" + RANGE;
        client.get(path).whenComplete((response, failure) -> scheduler.post(() -> {
            List<PriceJson.HistoryPoint> points = failure == null ? PriceJson.history(response) : List.of();
            if (failure == null) {
                cache.put(key, new Cached(points, System.currentTimeMillis()));
            }
            onLoaded.accept(points);
        }));
    }
}
