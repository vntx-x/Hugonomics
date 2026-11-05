package dev.hugomarket.container;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ContainerSnapshot(
        Component title,
        String menuType,
        int containerId,
        List<SlotSnapshot> slots,
        long capturedAt
) {
    public static ContainerSnapshot capture(AbstractContainerScreen<?> screen, LocalPlayer player) {
        AbstractContainerMenu menu = screen.getMenu();
        Container playerInventory = player.getInventory();
        List<SlotSnapshot> slots = new ArrayList<>(menu.slots.size());
        for (Slot slot : menu.slots) {
            slots.add(new SlotSnapshot(slot.index, slot.container == playerInventory, slot.getItem().copy()));
        }
        return new ContainerSnapshot(screen.getTitle(), menu.getClass().getSimpleName(), menu.containerId,
                List.copyOf(slots), System.currentTimeMillis());
    }

    public int contentHash() {
        int hash = containerId;
        for (SlotSnapshot slot : slots) {
            ItemStack stack = slot.stack();
            int item = stack.isEmpty() ? 0 : ItemStack.hashItemAndComponents(stack) * 31 + stack.getCount();
            hash = hash * 31 + item;
        }
        return hash;
    }
}
