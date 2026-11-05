package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Listing;

import java.util.Locale;

public record SearchQuery(String text, long minUnitCents, long maxUnitCents, boolean onlyEnchanted, boolean onlySpawner, Sort sort) {
    public enum Sort {
        UNIT_ASC,
        UNIT_DESC,
        EXPIRES_ASC,
        NEWEST
    }

    public static final SearchQuery EMPTY = new SearchQuery("", 0, Long.MAX_VALUE, false, false, Sort.UNIT_ASC);

    public SearchQuery withText(String newText) {
        return new SearchQuery(newText, minUnitCents, maxUnitCents, onlyEnchanted, onlySpawner, sort);
    }

    public SearchQuery withSort(Sort newSort) {
        return new SearchQuery(text, minUnitCents, maxUnitCents, onlyEnchanted, onlySpawner, newSort);
    }

    public SearchQuery toggleEnchanted() {
        return new SearchQuery(text, minUnitCents, maxUnitCents, !onlyEnchanted, onlySpawner, sort);
    }

    public SearchQuery toggleSpawner() {
        return new SearchQuery(text, minUnitCents, maxUnitCents, onlyEnchanted, !onlySpawner, sort);
    }

    public boolean isDefault() {
        return text.isBlank() && !onlyEnchanted && !onlySpawner && minUnitCents <= 0 && maxUnitCents == Long.MAX_VALUE;
    }

    public boolean matches(Listing listing) {
        if (onlyEnchanted && listing.item().enchantments().isEmpty() && listing.item().storedEnchantments().isEmpty()) {
            return false;
        }
        if (onlySpawner && !listing.item().id().equals("minecraft:spawner")) {
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
