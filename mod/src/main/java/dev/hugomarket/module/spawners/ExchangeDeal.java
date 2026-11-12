package dev.hugomarket.module.spawners;

record ExchangeDeal(String id, String offerId, String type, boolean offerSelling, boolean mineIsOffer, String otherPlayer,
                    ExchangeOffer.Rating otherRating, int count, long unitCents, boolean myTurn, Status status, int rounds,
                    boolean rated, long updatedAt) {
    enum Status {
        OPEN, ACCEPTED, DECLINED
    }

    boolean iSell() {
        return offerSelling == mineIsOffer;
    }

    String signature() {
        return status + "|" + myTurn + "|" + rounds;
    }
}
