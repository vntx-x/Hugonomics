package dev.hugomarket.module.containervalue;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.store.ContainerValuation;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.OverlayProvider;
import dev.hugomarket.ui.PanelPlacement;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ContainerValueModule implements Module, OverlayProvider {
    private static final int PANEL_WIDTH = 150;
    private static final Set<ScreenKind> VALUED = EnumSet.of(ScreenKind.VANILLA_CONTAINER, ScreenKind.ENDERCHEST, ScreenKind.SELL_GUI);

    private ModuleContext context;
    private ContainerValuePanel panel;
    private AbstractContainerScreen<?> panelScreen;

    @Override
    public String id() {
        return "containervalue";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
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
        if (screen instanceof AnvilScreen || isKnownHugoTitle(screen.getTitle().getString())) {
            return List.of();
        }
        panel = new ContainerValuePanel();
        panelScreen = screen;
        PanelPlacement.rightOf(screen, panel, PANEL_WIDTH, ContainerValuePanel.HEIGHT);
        ScreenUpdate current = context.screens().current();
        if (current != null && current.screen() == screen) {
            onScreenUpdate(current);
        }
        return List.of(panel);
    }

    private boolean isKnownHugoTitle(String title) {
        for (HugoGuiRules.ScreenRule rule : context.guiRules().screens()) {
            if (rule.title().equals(title) && !VALUED.contains(rule.kind())) {
                return true;
            }
        }
        return false;
    }

    private void onScreenUpdate(ScreenUpdate update) {
        if (panel == null || update.screen() != panelScreen) {
            return;
        }
        if (!VALUED.contains(update.kind())) {
            panel.setVisible(false);
            return;
        }
        panel.show(ContainerValuation.of(update.view(), context.market()));
        PanelPlacement.rightOf(update.screen(), panel, PANEL_WIDTH, ContainerValuePanel.HEIGHT);
    }
}
