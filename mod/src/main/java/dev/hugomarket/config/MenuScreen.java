package dev.hugomarket.config;

import dev.hugomarket.core.MenuActions;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleRegistry;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.Label;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

public final class MenuScreen extends Screen {
    private static final int WIDTH = 260;
    private static final int TILE_HEIGHT = 26;

    private final Screen parent;
    private final MenuPanel panel;

    public MenuScreen(Screen parent, ModuleRegistry registry, MenuActions actions) {
        super(Component.translatable("hugomarket.menu.title"));
        this.parent = parent;
        this.panel = new MenuPanel(registry, actions);
    }

    @Override
    protected void init() {
        int panelHeight = panel.preferredHeight();
        panel.setBounds((width - WIDTH) / 2, Math.max(10, (height - panelHeight) / 2), WIDTH, panelHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        panel.render(new Gfx(graphics), mouseX, mouseY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean handleKey(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return false;
    }

    //? if >=1.21.9 {
    /*@Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return panel.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return handleKey(event.key()) || super.keyPressed(event);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return handleKey(key) || super.keyPressed(key, scancode, modifiers);
    }
    //?}

    private static final class MenuPanel extends OverlayPanel {
        private final ModuleRegistry registry;
        private final List<Module> modules;
        private final List<Chip> toggles = new ArrayList<>();
        private final List<Label> names = new ArrayList<>();
        private final List<Chip> actionChips = new ArrayList<>();
        private final Label hint;

        MenuPanel(ModuleRegistry registry, MenuActions actions) {
            this.registry = registry;
            this.modules = registry.modules();
            for (MenuActions.Action action : actions.all()) {
                actionChips.add(add(new Chip(I18n.get(action.labelKey()), () -> false, action.run())));
            }
            for (Module module : modules) {
                names.add(add(new Label(() -> I18n.get("hugomarket.module." + module.id()), Theme.TEXT)));
                toggles.add(add(new Chip("", () -> registry.isEnabled(module.id()), () -> toggle(module))));
            }
            hint = add(new Label(() -> I18n.get("hugomarket.menu.hint")));
        }

        int preferredHeight() {
            int actionRows = (actionChips.size() + 1) / 2;
            return Theme.PADDING * 2 + 14 + actionRows * (Theme.CHIP_HEIGHT + Theme.GAP) + modules.size() * TILE_HEIGHT + 12;
        }

        private void toggle(Module module) {
            registry.setEnabled(module.id(), !registry.isEnabled(module.id()));
        }

        @Override
        protected void layout() {
            int innerX = x + Theme.PADDING;
            int innerWidth = width - Theme.PADDING * 2;
            int cursorY = y + Theme.PADDING + 14;
            int half = (innerWidth - Theme.GAP) / 2;
            for (int i = 0; i < actionChips.size(); i++) {
                actionChips.get(i).setBounds(innerX + (i % 2) * (half + Theme.GAP), cursorY + (i / 2) * (Theme.CHIP_HEIGHT + Theme.GAP), half, Theme.CHIP_HEIGHT);
            }
            cursorY += ((actionChips.size() + 1) / 2) * (Theme.CHIP_HEIGHT + Theme.GAP);
            for (int i = 0; i < modules.size(); i++) {
                names.get(i).setBounds(innerX, cursorY, innerWidth - 60, TILE_HEIGHT - 4);
                toggles.get(i).setBounds(innerX + innerWidth - 56, cursorY + (TILE_HEIGHT - 4 - Theme.CHIP_HEIGHT) / 2, 56, Theme.CHIP_HEIGHT);
                cursorY += TILE_HEIGHT;
            }
            hint.setBounds(innerX, cursorY, innerWidth, 12);
        }

        @Override
        public void render(Gfx gfx, int mouseX, int mouseY) {
            super.render(gfx, mouseX, mouseY);
            gfx.text(I18n.get("hugomarket.menu.title"), x + Theme.PADDING, y + Theme.PADDING, Theme.ACCENT);
            for (int i = 0; i < modules.size(); i++) {
                Chip chip = toggles.get(i);
                boolean enabled = registry.isEnabled(modules.get(i).id());
                boolean running = registry.isRunning(modules.get(i).id());
                String state = enabled ? (running ? I18n.get("hugomarket.menu.on") : I18n.get("hugomarket.menu.restart")) : I18n.get("hugomarket.menu.off");
                gfx.textNoShadow(state, chip.x() + (chip.width() - gfx.textWidth(state)) / 2, chip.y() + 2, enabled ? Theme.TEXT : Theme.TEXT_DIM);
            }
        }
    }
}
