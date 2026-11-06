package dev.hugomarket.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public final class GermanItemNames {
    // Hugo's AH search matches the German item names, whatever language the client runs in.
    private static final String LANGUAGE = "de_de";
    private static ClientLanguage german;

    private GermanItemNames() {
    }

    public static String of(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
        if (item == Items.AIR) {
            return null;
        }
        if (german == null) {
            german = ClientLanguage.loadFrom(Minecraft.getInstance().getResourceManager(), List.of(LANGUAGE), false);
        }
        String key = item.getDescriptionId();
        return german.has(key) ? german.getOrDefault(key, key) : null;
    }
}
