package dev.hugomarket.model;

public record Listing(
        int slot,
        ItemView item,
        ItemKey key,
        long priceCents,
        String seller,
        long expiresInSeconds,
        ListingStatus status,
        boolean own
) {
    public static final String OWN_SELLER = "Du";

    public long unitPriceCents() {
        return Math.round((double) priceCents / item.count());
    }

    public boolean isMarketListing() {
        return status == ListingStatus.ACTIVE && !own;
    }
}
