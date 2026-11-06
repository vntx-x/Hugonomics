package dev.hugomarket.store;

import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.net.BackendClient;

public final class RemoteListingsFeed {
    private static final long MAX_AGE_MS = 6 * 60 * 60_000L;
    private static final long FULL_REFRESH_MS = 10 * 60_000L;
    private static final long CLOCK_OVERLAP_MS = 3_000;

    private final BackendClient client;
    private final ClientScheduler scheduler;
    private final MarketSession market;
    private final RemoteListingsCache cache = new RemoteListingsCache(MAX_AGE_MS);
    private boolean fetching;
    private long lastFetchedAt;
    private long lastFullAt;
    private long serverCursor;

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
        boolean full = serverCursor == 0 || now - lastFullAt >= FULL_REFRESH_MS;
        String path = "/v1/listings?maxAge=" + MAX_AGE_MS + (full ? "" : "&since=" + (serverCursor - CLOCK_OVERLAP_MS));
        client.get(path).whenComplete((result, error) -> scheduler.post(() -> {
            fetching = false;
            if (error != null) {
                return;
            }
            long received = System.currentTimeMillis();
            lastFetchedAt = received;
            if (result.has("serverTime")) {
                serverCursor = result.get("serverTime").getAsLong();
            }
            if (full) {
                lastFullAt = received;
                cache.replace(ListingsJson.parse(result, received));
            } else {
                cache.merge(ListingsJson.parse(result, received));
            }
            market.replaceRemoteListings(cache.active(received), received);
        }));
    }
}
