package dev.hugomarket.store;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Fees {
    private static final String RESOURCE = "/assets/hugomarket/fees.json";

    public record Tier(long upToCents, double percent) {
        boolean covers(long cents) {
            return upToCents < 0 || cents <= upToCents;
        }
    }

    private final double listingFeePercent;
    private final List<Tier> saleTaxTiers;
    private final double orderFeePercent;

    public Fees(double listingFeePercent, List<Tier> saleTaxTiers, double orderFeePercent) {
        this.listingFeePercent = listingFeePercent;
        this.saleTaxTiers = List.copyOf(saleTaxTiers);
        this.orderFeePercent = orderFeePercent;
    }

    public static Fees load() {
        InputStream in = Fees.class.getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new IllegalStateException("missing " + RESOURCE);
        }
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Fees parse(JsonObject json) {
        List<Tier> tiers = new ArrayList<>();
        JsonArray array = json.getAsJsonArray("auctionSaleTaxTiers");
        for (JsonElement element : array) {
            JsonObject tier = element.getAsJsonObject();
            tiers.add(new Tier(tier.get("upToCents").getAsLong(), tier.get("percent").getAsDouble()));
        }
        return new Fees(json.get("auctionListingFeePercent").getAsDouble(), tiers, json.get("orderCreationFeePercent").getAsDouble());
    }

    public long listingFee(long priceCents) {
        return percentOf(priceCents, listingFeePercent);
    }

    public double saleTaxPercent(long priceCents) {
        for (Tier tier : saleTaxTiers) {
            if (tier.covers(priceCents)) {
                return tier.percent();
            }
        }
        return saleTaxTiers.get(saleTaxTiers.size() - 1).percent();
    }

    public long saleTax(long priceCents) {
        return percentOf(priceCents, saleTaxPercent(priceCents));
    }

    public long netAfterSale(long priceCents) {
        return priceCents - saleTax(priceCents);
    }

    public long orderCreationFee(long depositCents) {
        return percentOf(depositCents, orderFeePercent);
    }

    private static long percentOf(long cents, double percent) {
        return Math.round(cents * percent / 100.0);
    }
}
