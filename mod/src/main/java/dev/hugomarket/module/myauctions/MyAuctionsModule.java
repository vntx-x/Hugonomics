package dev.hugomarket.module.myauctions;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.store.Fees;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.OverlayProvider;
import dev.hugomarket.ui.PanelPlacement;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;

public final class MyAuctionsModule implements Module, OverlayProvider {
    private static final int PANEL_WIDTH = 170;
    private static final int PANEL_OFFSET_Y = 230;

    private ModuleContext context;
    private AhListingParser parser;
    private Fees fees;
    private MyAuctionsPanel panel;
    private AbstractContainerScreen<?> panelScreen;

    @Override
    public String id() {
        return "myauctions";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        parser = new AhListingParser(context.guiRules());
        fees = Fees.load();
        context.screens().onUpdate(this::onScreenUpdate);
        context.screens().onClose(() -> panel = null);
        context.overlays().addProvider(this);
    }

    @Override
    public void onDisable() {
        context.overlays().removeProvider(this);
        panel = null;
    }

    @Override
    public List<OverlayPanel> createPanels(AbstractContainerScreen<?> screen) {
        if (!screen.getTitle().getString().equals(context.guiRules().rule(ScreenKind.AH_LIST).title())) {
            return List.of();
        }
        panel = new MyAuctionsPanel();
        panelScreen = screen;
        PanelPlacement.rightOf(screen, panel, PANEL_WIDTH, MyAuctionsPanel.HEIGHT, PANEL_OFFSET_Y);
        ScreenUpdate current = context.screens().current();
        if (current != null && current.screen() == screen) {
            onScreenUpdate(current);
        }
        return List.of(panel);
    }

    private void onScreenUpdate(ScreenUpdate update) {
        if (panel == null || update.screen() != panelScreen) {
            return;
        }
        if (update.kind() != ScreenKind.AH_LIST) {
            panel.setVisible(false);
            return;
        }
        AhListingParser.AhPage page = parser.parse(update.view());
        if (page.isMarket()) {
            panel.setVisible(false);
            return;
        }
        panel.show(OwnAuctionSummary.of(page.view(), page.listings(), context.market(), fees), page.totals());
    }
}
