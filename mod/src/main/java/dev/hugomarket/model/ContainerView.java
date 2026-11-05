package dev.hugomarket.model;

import java.util.List;
import java.util.stream.Stream;

public record ContainerView(String title, String menuType, int containerSlots, List<SlotView> slots) {
    public Stream<SlotView> containerItems() {
        return slots.stream().filter(s -> !s.playerInventory() && !s.isEmpty());
    }

    public SlotView slot(int index) {
        for (SlotView slot : slots) {
            if (slot.index() == index) {
                return slot;
            }
        }
        return null;
    }
}
