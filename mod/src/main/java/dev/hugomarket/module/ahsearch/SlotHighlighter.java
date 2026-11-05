package dev.hugomarket.module.ahsearch;

import dev.hugomarket.mixin.AbstractContainerScreenAccessor;
import dev.hugomarket.model.Listing;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.BitSet;
import java.util.List;

final class SlotHighlighter {
    private static final int SLOT_SIZE = 16;

    private final AbstractContainerScreen<?> screen;
    private final AhSearchState state;
    private final BitSet matching = new BitSet();
    private final BitSet listed = new BitSet();

    SlotHighlighter(AbstractContainerScreen<?> screen, AhSearchState state) {
        this.screen = screen;
        this.state = state;
        refresh();
    }

    void refresh() {
        matching.clear();
        listed.clear();
        List<Listing> page = state.currentPage();
        for (Listing listing : page) {
            listed.set(listing.slot());
            if (state.matchesCurrentPage(listing)) {
                matching.set(listing.slot());
            }
        }
    }

    void render(Gfx gfx) {
        if (state.query().isDefault() || listed.isEmpty()) {
            return;
        }
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int left = accessor.hugomarket$leftPos();
        int top = accessor.hugomarket$topPos();
        for (Slot slot : screen.getMenu().slots) {
            if (!listed.get(slot.index)) {
                continue;
            }
            int x = left + slot.x;
            int y = top + slot.y;
            if (matching.get(slot.index)) {
                gfx.border(x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, Theme.MATCH_FRAME);
            } else {
                gfx.fill(x, y, SLOT_SIZE, SLOT_SIZE, Theme.DIM_SLOT);
            }
        }
    }
}
