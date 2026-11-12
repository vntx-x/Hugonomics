package dev.hugomarket.module.spawners;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExchangeTest {
    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static ExchangeDeal deal(String id, String status, boolean myTurn, int rounds, boolean owner) {
        return ExchangeJson.deal(json("""
                {"id": "%s", "offerId": "o1", "type": "skeleton", "offerSide": "sell", "role": "%s", "otherPlayer": "Bob",
                 "otherRating": {"average": 0, "count": 0}, "count": 8, "unitCents": 2700000000, "myTurn": %s,
                 "status": "%s", "rounds": %d, "rated": false, "createdAt": 1, "updatedAt": 2}
                """.formatted(id, owner ? "owner" : "from", myTurn, status, rounds)));
    }

    @Test
    void parsesOffersWithRatings() {
        List<ExchangeOffer> offers = ExchangeJson.offers(json("""
                {"serverTime": 1, "offers": [
                 {"id": "a", "type": "skeleton", "side": "sell", "count": 8, "unitCents": 2750000000, "player": "Alice",
                  "rating": {"average": 4.5, "count": 2}, "createdAt": 10, "expiresAt": 20},
                 {"id": "b", "type": "cow", "side": "buy", "count": 1, "unitCents": 1500000000, "player": "Bob",
                  "rating": {"average": 0, "count": 0}, "createdAt": 11, "expiresAt": 21}]}
                """));
        assertEquals(2, offers.size());
        assertTrue(offers.get(0).selling());
        assertEquals(new ExchangeOffer.Rating(4.5, 2), offers.get(0).rating());
        assertFalse(offers.get(1).selling());
        assertEquals(ExchangeOffer.Rating.NONE, offers.get(1).rating());
    }

    @Test
    void sellerOfASellOfferSellsAndTheRequesterBuys() {
        assertTrue(deal("d", "open", true, 1, true).iSell());
        assertFalse(deal("d", "open", false, 1, false).iSell());
    }

    @Test
    void notifiesOnlyAboutChangesAfterTheFirstPoll() {
        DealEvents events = new DealEvents();
        assertTrue(events.update(List.of(deal("old", "open", true, 1, true))).isEmpty());
        List<DealEvents.Event> fresh = events.update(List.of(deal("old", "open", true, 1, true), deal("new", "open", true, 1, true)));
        assertEquals(List.of(DealEvents.Kind.NEW_REQUEST), fresh.stream().map(DealEvents.Event::kind).toList());
        List<DealEvents.Event> answered = events.update(List.of(deal("old", "accepted", false, 1, true), deal("new", "open", true, 1, true)));
        assertEquals(List.of(DealEvents.Kind.ACCEPTED), answered.stream().map(DealEvents.Event::kind).toList());
    }

    @Test
    void ownActionsAreAcknowledgedAndCountersFromTheOtherSideNotify() {
        DealEvents events = new DealEvents();
        events.update(List.of());
        ExchangeDeal mine = deal("d", "open", false, 1, false);
        events.acknowledge(mine);
        assertTrue(events.update(List.of(mine)).isEmpty());
        List<DealEvents.Event> counter = events.update(List.of(deal("d", "open", true, 2, false)));
        assertEquals(List.of(DealEvents.Kind.COUNTER), counter.stream().map(DealEvents.Event::kind).toList());
    }
}
