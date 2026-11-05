package dev.hugomarket.model;

public record ItemKey(String base, String variant, boolean renamed) {
    @Override
    public String toString() {
        return variant.isEmpty() ? base : base + "#" + variant;
    }

    public static ItemKey parse(String text) {
        int hash = text.indexOf('#');
        return hash < 0 ? new ItemKey(text, "", false) : new ItemKey(text.substring(0, hash), text.substring(hash + 1), false);
    }
}
