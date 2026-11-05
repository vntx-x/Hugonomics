package dev.hugomarket.module.market;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.module.sync.PriceJson;
import dev.hugomarket.net.BackendClient;
import dev.hugomarket.store.MarketSession;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class MarketData {
    private static final long ITEMS_TTL_MS = 60_000;
    private static final int PRICE_BATCH = 200;

    private final BackendClient client;
    private final ClientScheduler scheduler;
    private final MarketSession market;
    private final List<Runnable> listeners = new ArrayList<>();
    private List<PriceJson.ItemSummary> items = List.of();
    private long itemsLoadedAt;
    private boolean loadingItems;
    private boolean loadingPrices;
    private String error = "";

    public MarketData(BackendClient client, ClientScheduler scheduler, MarketSession market) {
        this.client = client;
        this.scheduler = scheduler;
        this.market = market;
    }

    public void onChange(Runnable listener) {
        listeners.add(listener);
    }

    public List<PriceJson.ItemSummary> items() {
        return items;
    }

    public String error() {
        return error;
    }

    public boolean isLoading() {
        return loadingItems || loadingPrices;
    }

    public void refreshItems(long now) {
        if (client == null || loadingItems || now - itemsLoadedAt < ITEMS_TTL_MS) {
            return;
        }
        loadingItems = true;
        client.get("/v1/items").whenComplete((response, failure) -> scheduler.post(() -> {
            loadingItems = false;
            if (failure != null) {
                error = failure.getMessage() == null ? failure.toString() : failure.getMessage();
                notifyChange();
                return;
            }
            error = "";
            items = List.copyOf(PriceJson.items(response));
            itemsLoadedAt = now;
            notifyChange();
            fetchPrices(items);
        }));
    }

    private void fetchPrices(List<PriceJson.ItemSummary> wanted) {
        if (client == null || loadingPrices || wanted.isEmpty()) {
            return;
        }
        JsonObject body = new JsonObject();
        JsonArray keys = new JsonArray();
        for (int i = 0; i < wanted.size() && i < PRICE_BATCH; i++) {
            keys.add(wanted.get(i).key());
        }
        body.add("keys", keys);
        loadingPrices = true;
        client.post("/v1/prices/batch", body).whenComplete((response, failure) -> scheduler.post(() -> {
            loadingPrices = false;
            if (failure != null) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : response.getAsJsonObject("prices").entrySet()) {
                if (!entry.getValue().isJsonNull()) {
                    market.backend().put(entry.getKey(), PriceJson.stats(entry.getValue().getAsJsonObject()));
                }
            }
            market.backendUpdated();
            notifyChange();
        }));
    }

    public void loadHistory(String key, String range, Consumer<List<PriceJson.HistoryPoint>> onLoaded) {
        if (client == null) {
            onLoaded.accept(List.of());
            return;
        }
        String path = "/v1/items/" + URLEncoder.encode(key, StandardCharsets.UTF_8) + "/history?range=" + range;
        client.get(path).whenComplete((response, failure) -> scheduler.post(() -> {
            if (failure != null) {
                error = failure.getMessage() == null ? failure.toString() : failure.getMessage();
                onLoaded.accept(List.of());
                return;
            }
            onLoaded.accept(PriceJson.history(response));
        }));
    }

    private void notifyChange() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
