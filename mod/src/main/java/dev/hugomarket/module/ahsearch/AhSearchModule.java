package dev.hugomarket.module.ahsearch;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.OverlayProvider;
import dev.hugomarket.ui.PanelPlacement;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AhSearchModule implements Module, OverlayProvider {
    private static final int PANEL_WIDTH = 170;

    private final AhSearchState state = new AhSearchState();
    private ModuleContext context;
    private AhListingParser parser;
    private AhSearchPanel panel;
    private boolean ahOpen;

    @Override
    public String id() {
        return "ahsearch";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        parser = new AhListingParser(context.guiRules());
        context.screens().onUpdate(this::onScreenUpdate);
        context.screens().onClose(this::onScreenClosed);
        context.overlays().addProvider(this);
    }

    @Override
    public void onDisable() {
        context.overlays().removeProvider(this);
        state.pageClosed();
    }

    @Override
    public List<OverlayPanel> createPanels(AbstractContainerScreen<?> screen) {
        ScreenUpdate current = context.screens().current();
        boolean isAh = ahOpen || (current != null && current.kind() == ScreenKind.AH_LIST && current.screen() == screen);
        if (!isAh && !looksLikeAh(screen)) {
            panel = null;
            return List.of();
        }
        panel = new AhSearchPanel(screen, state);
        PanelPlacement.rightOf(screen, panel, PANEL_WIDTH);
        return List.of(panel);
    }

    private boolean looksLikeAh(AbstractContainerScreen<?> screen) {
        return screen.getTitle().getString().equals(context.guiRules().rule(ScreenKind.AH_LIST).title());
    }

    private void onScreenUpdate(ScreenUpdate update) {
        if (update.kind() != ScreenKind.AH_LIST) {
            if (ahOpen) {
                ahOpen = false;
                state.pageClosed();
            }
            return;
        }
        ahOpen = true;
        AhListingParser.AhPage page = parser.parse(update.view());
        List<ItemStack> stacks = new ArrayList<>(page.listings().size());
        for (Listing listing : page.listings()) {
            stacks.add(update.snapshot().slots().get(listing.slot()).stack());
        }
        state.pageSeen(page.listings(), stacks, update.snapshot().capturedAt());
        if (panel != null) {
            PanelPlacement.rightOf(update.screen(), panel, PANEL_WIDTH);
        }
    }

    private void onScreenClosed() {
        if (ahOpen) {
            ahOpen = false;
            state.pageClosed();
        }
        panel = null;
    }
}
