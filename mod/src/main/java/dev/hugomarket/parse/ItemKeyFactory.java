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
        String base = customModel ? customBase(item) : stripVanilla(item.id());
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
        if (item.hasContents()) {
            features.add(ItemKey.FILLED);
        }
        features.sort(null);
        boolean renamed = item.customName() != null && !customModel;
        return new ItemKey(base, String.join(";", features), renamed);
    }

    private static String customBase(ItemView item) {
        String model = item.itemModel();
        int colon = model.indexOf(':');
        String namespace = colon < 0 ? "custom" : model.substring(0, colon);
        String path = colon < 0 ? model : model.substring(colon + 1);
        boolean hashedPath = path.contains("/") && path.matches(".*[0-9a-f]{12,}.*");
        if (hashedPath) {
            String slug = item.customName() == null ? "" : NameSlug.of(item.customName());
            if (!slug.isEmpty()) {
                return namespace + ":" + slug;
            }
            return namespace + ":" + path.substring(path.lastIndexOf('/') + 1);
        }
        return model;
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
