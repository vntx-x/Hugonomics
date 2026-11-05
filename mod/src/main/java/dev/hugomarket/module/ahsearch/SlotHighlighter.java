package dev.hugomarket.module.ahsearch;

import dev.hugomarket.mixin.AbstractContainerScreenAccessor;
import dev.hugomarket.model.Listing;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.PriceStats;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.BitSet;
import java.util.List;

final class SlotHighlighter {
    private static final int SLOT_SIZE = 16;
    private static final double BARGAIN_FACTOR = 0.85;
    private static final int MIN_LISTINGS_FOR_BARGAIN = 3;

    private final AbstractContainerScreen<?> screen;
    private final AhSearchState state;
    private final MarketSession market;
    private final BitSet matching = new BitSet();
    private final BitSet listed = new BitSet();
    private final BitSet bargain = new BitSet();

    SlotHighlighter(AbstractContainerScreen<?> screen, AhSearchState state, MarketSession market) {
        this.screen = screen;
        this.state = state;
        this.market = market;
        refresh();
    }

    void refresh() {
        matching.clear();
        listed.clear();
        bargain.clear();
        List<Listing> page = state.currentPage();
        for (Listing listing : page) {
            listed.set(listing.slot());
            if (state.matchesCurrentPage(listing)) {
                matching.set(listing.slot());
            }
            if (isBargain(listing)) {
                bargain.set(listing.slot());
            }
        }
    }

    private boolean isBargain(Listing listing) {
        PriceStats stats = market.stats(listing.key());
        if (stats == null || stats.listings() < MIN_LISTINGS_FOR_BARGAIN) {
            return false;
        }
        return listing.unitPriceCents() <= stats.medianUnitCents() * BARGAIN_FACTOR;
    }

    void render(Gfx gfx) {
        boolean filtering = !state.query().isDefault();
        if (listed.isEmpty() || (!filtering && bargain.isEmpty())) {
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
            if (filtering && !matching.get(slot.index)) {
                gfx.fill(x, y, SLOT_SIZE, SLOT_SIZE, Theme.DIM_SLOT);
                continue;
            }
            if (bargain.get(slot.index)) {
                gfx.border(x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, Theme.BARGAIN_FRAME);
            } else if (filtering) {
                gfx.border(x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, Theme.MATCH_FRAME);
            }
        }
    }
}
