package dev.hugomarket.model;

import java.util.List;
import java.util.Map;

public record ItemView(
        String id,
        int count,
        String displayName,
        List<String> lore,
        Map<String, Integer> enchantments,
        Map<String, Integer> storedEnchantments,
        String potion,
        String itemModel,
        List<Float> customModelData,
        String spawnerEntity,
        String customName,
        Map<String, String> bukkitValues,
        List<ItemView> contents,
        boolean glint
) {
    public static final String SECURED_KEY = "minecraft:secured";

    public ItemView(String id, int count, String displayName, List<String> lore, Map<String, Integer> enchantments,
                    Map<String, Integer> storedEnchantments, String potion, String itemModel, List<Float> customModelData,
                    String spawnerEntity, String customName, Map<String, String> bukkitValues, List<ItemView> contents) {
        this(id, count, displayName, lore, enchantments, storedEnchantments, potion, itemModel, customModelData, spawnerEntity,
                customName, bukkitValues, contents, false);
    }

    public String secured() {
        return bukkitValues.get(SECURED_KEY);
    }

    public boolean hasContents() {
        return !contents.isEmpty();
    }
}
