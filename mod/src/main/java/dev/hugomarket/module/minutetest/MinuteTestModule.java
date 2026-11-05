package dev.hugomarket.module.minutetest;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.store.MinuteTest;
import dev.hugomarket.store.MinuteTests;
import dev.hugomarket.store.SellStats;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.OverlayProvider;
import dev.hugomarket.ui.PanelPlacement;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;

public final class MinuteTestModule implements Module, OverlayProvider {
    private static final int PANEL_WIDTH = 170;
    private static final int PANEL_OFFSET_Y = 134;

    private ModuleContext context;
    private final MinuteTest test = new MinuteTest();
    private MinuteTests saved;
    private MinuteTestPanel panel;

    @Override
    public String id() {
        return "minutetest";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        saved = new MinuteTests(context.dataDir().resolve("minutetests.json"));
        saved.load();
        context.scheduler().onTick(mc -> tick());
        context.overlays().addProvider(this);
    }

    public void attach(SellStats stats) {
        stats.onPayout(p -> test.sale(p.ts(), p.baseCents(), p.bonusCents(), p.totalCents(), p.multiplier()));
    }

    @Override
    public void onDisable() {
        context.overlays().removeProvider(this);
        test.cancel();
    }

    @Override
    public List<OverlayPanel> createPanels(AbstractContainerScreen<?> screen) {
        HugoGuiRules.ScreenRule rule = context.guiRules().rule(ScreenKind.SELL_GUI);
        if (rule == null || !rule.title().equals(screen.getTitle().getString())) {
            return List.of();
        }
        panel = new MinuteTestPanel(test, saved);
        PanelPlacement.rightOf(screen, panel, PANEL_WIDTH, MinuteTestPanel.HEIGHT, PANEL_OFFSET_Y);
        return List.of(panel);
    }

    private void tick() {
        if (!test.isRunning()) {
            return;
        }
        test.tick(System.currentTimeMillis());
        MinuteTest.Result result = test.finished();
        if (result != null) {
            MinuteTest.Result named = saved.add(result);
            if (panel != null) {
                panel.finished(named);
            }
        }
    }
}
