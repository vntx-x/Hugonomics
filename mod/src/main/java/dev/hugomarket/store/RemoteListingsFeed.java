package dev.hugomarket.store;

import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.net.BackendClient;

public final class RemoteListingsFeed {
    private static final long MAX_AGE_MS = 20 * 60_000;

    private final BackendClient client;
    private final ClientScheduler scheduler;
    private final MarketSession market;
    private boolean fetching;
    private long lastFetchedAt;

    public RemoteListingsFeed(BackendClient client, ClientScheduler scheduler, MarketSession market) {
        this.client = client;
        this.scheduler = scheduler;
        this.market = market;
    }

    public long lastFetchedAt() {
        return lastFetchedAt;
    }

    public void request(long maxStaleMs) {
        long now = System.currentTimeMillis();
        if (fetching || now - lastFetchedAt < maxStaleMs || !client.available()) {
            return;
        }
        fetching = true;
        client.get("/v1/listings?maxAge=" + MAX_AGE_MS).whenComplete((result, error) -> scheduler.post(() -> {
            fetching = false;
            if (error != null) {
                return;
            }
            long received = System.currentTimeMillis();
            lastFetchedAt = received;
            market.replaceRemoteListings(ListingsJson.parse(result, received), received);
        }));
    }
}
