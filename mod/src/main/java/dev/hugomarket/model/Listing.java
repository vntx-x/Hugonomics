package dev.hugomarket.model;

public record Listing(int slot, ItemView item, ItemKey key, long priceCents, String seller, long expiresInSeconds) {
    public long unitPriceCents() {
        return Math.round((double) priceCents / item.count());
    }
}
