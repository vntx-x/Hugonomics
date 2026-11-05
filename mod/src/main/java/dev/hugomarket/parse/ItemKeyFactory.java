package dev.hugomarket.parse;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.ItemView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ItemKeyFactory {
    private static final String VANILLA = "minecraft:";

    private ItemKeyFactory() {
    }

    public static ItemKey of(ItemView item) {
        boolean customModel = item.itemModel() != null && !item.itemModel().startsWith(VANILLA);
        String base = customModel ? item.itemModel() : stripVanilla(item.id());
        List<String> features = new ArrayList<>();
        addEnchantments(features, item.enchantments());
        addEnchantments(features, item.storedEnchantments());
        if (item.potion() != null) {
            features.add(stripVanilla(item.potion()));
        }
        if (item.spawnerEntity() != null) {
            features.add(stripVanilla(item.spawnerEntity()));
        }
        if (!customModel && !item.customModelData().isEmpty()) {
            features.add("cmd" + Math.round(item.customModelData().get(0)));
        }
        features.sort(null);
        boolean renamed = item.customName() != null && !customModel;
        return new ItemKey(base, String.join(";", features), renamed);
    }

    private static void addEnchantments(List<String> features, Map<String, Integer> enchantments) {
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            features.add(stripVanilla(entry.getKey()) + entry.getValue());
        }
    }

    private static String stripVanilla(String id) {
        return id.startsWith(VANILLA) ? id.substring(VANILLA.length()) : id;
    }
}
