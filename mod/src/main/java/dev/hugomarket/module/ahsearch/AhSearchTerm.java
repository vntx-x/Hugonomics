package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Listing;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class AhSearchTerm {
    private static final int MAX_LENGTH = 40;
    private static final String VANILLA = "minecraft:";
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final Pattern UNSAFE = Pattern.compile("[^\\p{L}\\p{N} '&+.-]");

    private AhSearchTerm() {
    }

    public static String of(Listing listing, Function<String, String> vanillaName) {
        String base = listing.key().base();
        String term;
        if (base.contains(":") || !listing.item().id().startsWith(VANILLA)) {
            term = listing.item().displayName();
        } else {
            String translated = vanillaName.apply(listing.item().id());
            term = translated == null ? base.replace('_', ' ') : translated;
        }
        term = UNSAFE.matcher(FORMATTING.matcher(term).replaceAll("")).replaceAll(" ").trim().replaceAll("\\s+", " ");
        if (term.length() > MAX_LENGTH) {
            term = term.substring(0, MAX_LENGTH).trim();
        }
        return term.toLowerCase(Locale.ROOT);
    }
}
