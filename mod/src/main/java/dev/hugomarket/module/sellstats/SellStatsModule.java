package dev.hugomarket.module.sellstats;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.parse.ChatPatterns;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.store.SellStats;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.OverlayProvider;
import dev.hugomarket.ui.PanelPlacement;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;

public final class SellStatsModule implements Module, OverlayProvider {
    private static final int PANEL_WIDTH = 150;
    private static final int PANEL_OFFSET_Y = 70;

    private ModuleContext context;
    private SellStats stats;
    private boolean enabled;

    @Override
    public String id() {
        return "sellstats";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        enabled = true;
        long now = System.currentTimeMillis();
        stats = new SellStats(context.dataDir(), now);
        stats.load(now);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onGameMessage(message.getString()));
        context.overlays().addProvider(this);
    }

    @Override
    public void onDisable() {
        enabled = false;
        context.overlays().removeProvider(this);
    }

    public SellStats stats() {
        return stats;
    }

    private void onGameMessage(String text) {
        if (!enabled || !context.server().active()) {
            return;
        }
        ChatPatterns.payout(text).ifPresent(p -> stats.record(
                new SellStats.Payout(System.currentTimeMillis(), p.totalCents(), p.baseCents(), p.bonusCents(), p.multiplier())));
    }

    @Override
    public List<OverlayPanel> createPanels(AbstractContainerScreen<?> screen) {
        HugoGuiRules.ScreenRule rule = context.guiRules().rule(ScreenKind.SELL_GUI);
        if (rule == null || !rule.title().equals(screen.getTitle().getString())) {
            return List.of();
        }
        SellStatsPanel panel = new SellStatsPanel(stats);
        PanelPlacement.rightOf(screen, panel, PANEL_WIDTH, SellStatsPanel.HEIGHT, PANEL_OFFSET_Y);
        return List.of(panel);
    }

    void onScreenUpdate(ScreenUpdate update) {
    }
}
