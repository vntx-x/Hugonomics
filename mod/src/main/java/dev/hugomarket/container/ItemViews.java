package dev.hugomarket.container;

import dev.hugomarket.model.ItemView;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
//? if >=1.21.9 {
/*import net.minecraft.world.item.component.TypedEntityData;
*///?}
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ItemViews {
    private static final int MAX_DEPTH = 2;

    private ItemViews() {
    }

    public static ItemView of(ItemStack stack) {
        return of(stack, 0);
    }

    private static ItemView of(ItemStack stack, int depth) {
        ItemLore lore = stack.get(DataComponents.LORE);
        List<String> loreLines = new ArrayList<>(lore == null ? 0 : lore.lines().size());
        if (lore != null) {
            for (Component line : lore.lines()) {
                loreLines.add(line.getString());
            }
        }
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        String potionId = potion == null ? null
                : potion.potion().flatMap(Holder::unwrapKey).map(ResourceKeys::id).orElse(null);
        ResourceLocation model = stack.get(DataComponents.ITEM_MODEL);
        CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        return new ItemView(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.getCount(),
                stack.getHoverName().getString(),
                List.copyOf(loreLines),
                enchantments(stack.get(DataComponents.ENCHANTMENTS)),
                enchantments(stack.get(DataComponents.STORED_ENCHANTMENTS)),
                potionId,
                model == null ? null : model.toString(),
                modelData == null ? List.of() : List.copyOf(modelData.floats()),
                spawnerEntity(stack),
                customName == null ? null : customName.getString(),
                bukkitValues(stack.get(DataComponents.CUSTOM_DATA)),
                contents(stack.get(DataComponents.CONTAINER), depth));
    }

    private static Map<String, Integer> enchantments(ItemEnchantments enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> out = new TreeMap<>();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            entry.getKey().unwrapKey().ifPresent(key -> out.put(ResourceKeys.id(key), entry.getIntValue()));
        }
        return Map.copyOf(out);
    }

    private static String spawnerEntity(ItemStack stack) {
        //? if >=1.21.9 {
        /*TypedEntityData<?> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = data == null ? null : data.copyTagWithoutId();
        *///?} else {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = data == null ? null : data.copyTag();
        //?}
        if (tag == null) {
            return null;
        }
        String id = tag.getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity").getStringOr("id", "");
        return id.isEmpty() ? null : id;
    }

    private static Map<String, String> bukkitValues(CustomData data) {
        if (data == null) {
            return Map.of();
        }
        CompoundTag values = data.copyTag().getCompoundOrEmpty("PublicBukkitValues");
        if (values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : values.keySet()) {
            Tag tag = values.get(key);
            if (tag != null) {
                out.put(key, tag.asString().orElseGet(tag::toString));
            }
        }
        return Map.copyOf(out);
    }

    private static List<ItemView> contents(ItemContainerContents container, int depth) {
        if (container == null || depth >= MAX_DEPTH) {
            return List.of();
        }
        List<ItemView> out = new ArrayList<>();
        for (ItemStack stack : container.nonEmptyItems()) {
            out.add(of(stack, depth + 1));
        }
        return List.copyOf(out);
    }
}
