package dev.hugomarket.module.dumper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import dev.hugomarket.container.SlotSnapshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

import java.util.List;

final class ItemJson {
    private ItemJson() {
    }

    static JsonObject of(SlotSnapshot slot, Level level, LocalPlayer player, DynamicOps<JsonElement> ops) {
        ItemStack stack = slot.stack();
        JsonObject json = new JsonObject();
        json.addProperty("index", slot.index());
        json.addProperty("player", slot.playerInventory());
        json.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        json.addProperty("count", stack.getCount());
        json.addProperty("name", stack.getHoverName().getString());
        json.add("nameJson", ComponentJson.of(stack.getHoverName(), ops));
        json.add("lore", lines(loreLines(stack)));
        json.add("tooltip", lines(stack.getTooltipLines(Item.TooltipContext.of(level), player, TooltipFlag.NORMAL)));
        json.add("stack", ItemStack.CODEC.encodeStart(ops, stack).result().orElse(JsonNull.INSTANCE));
        return json;
    }

    private static List<Component> loreLines(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        return lore == null ? List.of() : lore.lines();
    }

    private static JsonArray lines(List<Component> components) {
        JsonArray array = new JsonArray(components.size());
        for (Component component : components) {
            array.add(component.getString());
        }
        return array;
    }
}
