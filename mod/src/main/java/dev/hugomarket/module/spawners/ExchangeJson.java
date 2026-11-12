package dev.hugomarket.module.spawners;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

final class ExchangeJson {
    private ExchangeJson() {
    }

    static List<ExchangeOffer> offers(JsonObject body) {
        List<ExchangeOffer> out = new ArrayList<>();
        for (JsonElement element : body.getAsJsonArray("offers")) {
            out.add(offer(element.getAsJsonObject()));
        }
        return List.copyOf(out);
    }

    static ExchangeOffer offer(JsonObject o) {
        return new ExchangeOffer(o.get("id").getAsString(), o.get("type").getAsString(), "sell".equals(o.get("side").getAsString()),
                o.get("count").getAsInt(), o.get("unitCents").getAsLong(), o.get("player").getAsString(),
                rating(o.getAsJsonObject("rating")), o.get("createdAt").getAsLong());
    }

    static List<ExchangeDeal> deals(JsonObject body) {
        List<ExchangeDeal> out = new ArrayList<>();
        for (JsonElement element : body.getAsJsonArray("requests")) {
            out.add(deal(element.getAsJsonObject()));
        }
        return List.copyOf(out);
    }

    static ExchangeDeal deal(JsonObject r) {
        return new ExchangeDeal(r.get("id").getAsString(), r.get("offerId").getAsString(), r.get("type").getAsString(),
                "sell".equals(r.get("offerSide").getAsString()), "owner".equals(r.get("role").getAsString()),
                r.get("otherPlayer").getAsString(), rating(r.getAsJsonObject("otherRating")), r.get("count").getAsInt(),
                r.get("unitCents").getAsLong(), r.get("myTurn").getAsBoolean(), status(r.get("status").getAsString()),
                r.get("rounds").getAsInt(), r.get("rated").getAsBoolean(), r.get("updatedAt").getAsLong());
    }

    private static ExchangeOffer.Rating rating(JsonObject rating) {
        if (rating == null || rating.get("count").getAsInt() == 0) {
            return ExchangeOffer.Rating.NONE;
        }
        return new ExchangeOffer.Rating(rating.get("average").getAsDouble(), rating.get("count").getAsInt());
    }

    private static ExchangeDeal.Status status(String status) {
        return switch (status) {
            case "accepted" -> ExchangeDeal.Status.ACCEPTED;
            case "declined" -> ExchangeDeal.Status.DECLINED;
            default -> ExchangeDeal.Status.OPEN;
        };
    }
}
