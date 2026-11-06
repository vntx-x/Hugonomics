package dev.hugomarket.ui;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.store.ListingsJson;
import net.minecraft.client.resources.language.I18n;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class ItemLabel {
    private static final String BOOK = "enchanted_book";
    private static final int MAX_CACHE = 2048;
    private static final Map<String, String> CACHE = new HashMap<>();

    private ItemLabel() {
    }

    public static String of(ItemKey key, String displayName) {
        if (!BOOK.equals(key.base()) || key.variant().isEmpty()) {
            return displayName;
        }
        String cached = CACHE.get(key.variant());
        if (cached != null) {
            return cached.isEmpty() ? displayName : cached;
        }
        if (CACHE.size() >= MAX_CACHE) {
            CACHE.clear();
        }
        String label = enchantments(key.variant());
        CACHE.put(key.variant(), label);
        return label.isEmpty() ? displayName : label;
    }

    private static String enchantments(String variant) {
        StringJoiner enchants = new StringJoiner(", ");
        for (Map.Entry<String, Integer> entry : ListingsJson.enchantments(variant).entrySet()) {
            String translation = "enchantment.minecraft." + entry.getKey();
            if (!I18n.exists(translation)) {
                continue;
            }
            String level = "enchantment.level." + entry.getValue();
            enchants.add(I18n.get(translation) + " " + (I18n.exists(level) ? I18n.get(level) : entry.getValue()));
        }
        return enchants.toString();
    }
}
