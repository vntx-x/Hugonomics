package dev.hugomarket.model;

public record Order(
        int slot,
        ItemView item,
        ItemKey key,
        String owner,
        long unitPriceCents,
        long delivered,
        long total,
        long paidOutCents,
        long paidTotalCents
) {
    public long open() {
        return total - delivered;
    }
}
