package dev.hugomarket.module.farm;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.store.ContainerCounts;
import dev.hugomarket.store.FarmSession;
import dev.hugomarket.store.Inflow;
import dev.hugomarket.store.SellStats;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.OverlayProvider;
import dev.hugomarket.ui.PanelPlacement;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FarmModule implements Module, OverlayProvider {
    private static final int PANEL_WIDTH = 170;
    private static final int PANEL_OFFSET_Y = 56;
    private static final Set<ScreenKind> CONTAINERS = EnumSet.of(ScreenKind.VANILLA_CONTAINER, ScreenKind.ENDERCHEST);

    private ModuleContext context;
    private FarmSession session;
    private final Inflow inflow = new Inflow();
    private Map<String, Integer> lastCounts;
    private int lastContainerId = -1;
    private FarmPanel panel;
    private AbstractContainerScreen<?> panelScreen;

    @Override
    public String id() {
        return "farm";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        session = new FarmSession(System.currentTimeMillis());
        context.screens().onUpdate(this::onScreenUpdate);
        context.screens().onClose(this::onClose);
        context.overlays().addProvider(this);
    }

    public void attach(SellStats stats) {
        stats.onPayout(p -> session.income(p.totalCents(), p.ts()));
    }

    @Override
    public void onDisable() {
        context.overlays().removeProvider(this);
        panel = null;
    }

    @Override
    public List<OverlayPanel> createPanels(AbstractContainerScreen<?> screen) {
        if (screen instanceof AnvilScreen || isHugoTitle(screen.getTitle().getString())) {
            return List.of();
        }
        panel = new FarmPanel(session, inflow, context.market());
        panel.setVisible(false);
        panelScreen = screen;
        PanelPlacement.rightOf(screen, panel, PANEL_WIDTH, FarmPanel.HEIGHT, PANEL_OFFSET_Y);
        return List.of(panel);
    }

    private boolean isHugoTitle(String title) {
        for (HugoGuiRules.ScreenRule rule : context.guiRules().screens()) {
            if (rule.title().equals(title) && rule.kind() != ScreenKind.ENDERCHEST) {
                return true;
            }
        }
        return false;
    }

    private void onScreenUpdate(ScreenUpdate update) {
        if (!CONTAINERS.contains(update.kind())) {
            if (panel != null && update.screen() == panelScreen) {
                panel.setVisible(false);
            }
            return;
        }
        Map<String, Integer> counts = ContainerCounts.of(update.view());
        if (update.snapshot().containerId() == lastContainerId && lastCounts != null) {
            session.collected(ContainerCounts.removed(lastCounts, counts), update.snapshot().capturedAt());
            inflow.added(ContainerCounts.added(lastCounts, counts));
        } else {
            inflow.open(update.snapshot().capturedAt());
        }
        lastContainerId = update.snapshot().containerId();
        lastCounts = counts;
        if (panel != null && update.screen() == panelScreen) {
            panel.setVisible(true);
        }
    }

    private void onClose() {
        lastContainerId = -1;
        lastCounts = null;
        panel = null;
    }
}
