package dev.hugomarket.container;

import net.minecraft.world.item.ItemStack;

public record SlotSnapshot(int index, boolean playerInventory, ItemStack stack) {
}
