package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Listing;

import java.util.Locale;
import java.util.function.Predicate;

public record SearchQuery(String text, long minUnitCents, long maxUnitCents, boolean onlyEnchanted, boolean onlyWatched,
                          boolean onlyDeals, Sort sort) {
    public enum Sort {
        UNIT_ASC,
        UNIT_DESC,
        EXPIRES_ASC,
        NEWEST,
        DEAL
    }

    public static final SearchQuery EMPTY = new SearchQuery("", 0, Long.MAX_VALUE, false, false, false, Sort.UNIT_ASC);
    public static final Predicate<String> NOTHING_WATCHED = key -> false;

    public SearchQuery withText(String newText) {
        return new SearchQuery(newText, minUnitCents, maxUnitCents, onlyEnchanted, onlyWatched, onlyDeals, sort);
    }

    public SearchQuery withSort(Sort newSort) {
        return new SearchQuery(text, minUnitCents, maxUnitCents, onlyEnchanted, onlyWatched, onlyDeals, newSort);
    }

    public SearchQuery toggleEnchanted() {
        return new SearchQuery(text, minUnitCents, maxUnitCents, !onlyEnchanted, onlyWatched, onlyDeals, sort);
    }

    public SearchQuery toggleWatched() {
        return new SearchQuery(text, minUnitCents, maxUnitCents, onlyEnchanted, !onlyWatched, onlyDeals, sort);
    }

    public SearchQuery toggleDeals() {
        return new SearchQuery(text, minUnitCents, maxUnitCents, onlyEnchanted, onlyWatched, !onlyDeals, sort);
    }

    public boolean isDefault() {
        return text.isBlank() && !onlyEnchanted && !onlyWatched && !onlyDeals && minUnitCents <= 0 && maxUnitCents == Long.MAX_VALUE;
    }

    public boolean matches(Listing listing) {
        return matches(listing, NOTHING_WATCHED);
    }

    public boolean matches(Listing listing, Predicate<String> isWatched) {
        if (onlyWatched && !isWatched.test(listing.key().toString())) {
            return false;
        }
        if (onlyEnchanted && listing.item().enchantments().isEmpty() && listing.item().storedEnchantments().isEmpty()) {
            return false;
        }
        long unit = listing.unitPriceCents();
        if (unit < minUnitCents || unit > maxUnitCents) {
            return false;
        }
        if (text.isBlank()) {
            return true;
        }
        String haystack = (listing.item().displayName() + " " + listing.key() + " " + listing.seller()).toLowerCase(Locale.ROOT);
        for (String token : text.toLowerCase(Locale.ROOT).trim().split("\\s+")) {
            if (!haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }
}
