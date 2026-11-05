package dev.hugomarket.container;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.SlotView;

import java.util.ArrayList;
import java.util.List;

public final class ContainerViews {
    private ContainerViews() {
    }

    public static ContainerView of(ContainerSnapshot snapshot) {
        List<SlotView> slots = new ArrayList<>(snapshot.slots().size());
        for (SlotSnapshot slot : snapshot.slots()) {
            slots.add(new SlotView(slot.index(), slot.playerInventory(),
                    slot.stack().isEmpty() ? null : ItemViews.of(slot.stack())));
        }
        return new ContainerView(snapshot.title().getString(), snapshot.menuType(), snapshot.containerSlots(), List.copyOf(slots));
    }
}
