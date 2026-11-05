package dev.hugomarket.module.calc;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.ui.KeyBindings;
import dev.hugomarket.ui.KeyMatch;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.OverlayProvider;
import dev.hugomarket.ui.PanelPlacement;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class CalcModule implements Module, OverlayProvider {
    private static final String KEY_NAME = "key.hugomarket.calculator";

    private final CalcState state = new CalcState();
    private ModuleContext context;
    private KeyMapping key;
    private CalcPanel panel;

    @Override
    public String id() {
        return "calculator";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        key = KeyBindings.register(KEY_NAME, GLFW.GLFW_KEY_N);
        context.scheduler().onTick(this::tick);
        context.overlays().addProvider(this);
        context.overlays().addKeyHook(this::onScreenKey);
    }

    @Override
    public void onDisable() {
        context.overlays().removeProvider(this);
        state.setOpen(false);
    }

    @Override
    public List<OverlayPanel> createPanels(AbstractContainerScreen<?> screen) {
        panel = new CalcPanel(state, this::closePanel);
        PanelPlacement.leftOf(screen, panel, CalcPanel.WIDTH, CalcPanel.preferredHeight());
        if (state.isOpen()) {
            panel.focus();
        }
        return List.of(panel);
    }

    private void tick(Minecraft client) {
        while (key.consumeClick()) {
            if (client.screen == null && client.player != null) {
                state.setOpen(true);
                client.setScreen(new CalcScreen(state));
            }
        }
    }

    private boolean onScreenKey(int keyCode, int scancode, int modifiers) {
        if (panel == null || !KeyMatch.matches(key, keyCode, scancode)) {
            return false;
        }
        if (state.isOpen()) {
            closePanel();
        } else {
            state.setOpen(true);
            panel.setVisible(true);
            panel.focus();
        }
        return true;
    }

    private void closePanel() {
        state.setOpen(false);
        if (panel != null) {
            panel.unfocus();
            panel.setVisible(false);
        }
    }
}
