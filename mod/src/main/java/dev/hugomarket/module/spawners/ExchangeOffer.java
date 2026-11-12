package dev.hugomarket.module.spawners;

record ExchangeOffer(String id, String type, boolean selling, int count, long unitCents, String player, Rating rating,
                     long createdAt) {
    record Rating(double average, int count) {
        static final Rating NONE = new Rating(0, 0);
    }
}
