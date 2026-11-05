package dev.hugomarket.config;

import dev.hugomarket.HugoMarket;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

public final class MenuScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int MARGIN = 12;

    private final Screen parent;
    private final MenuPanel panel;

    public MenuScreen(Screen parent, ModuleRegistry registry, MenuActions actions) {
        super(Component.translatable("hugomarket.menu.title"));
        this.parent = parent;
        this.panel = new MenuPanel(registry, actions);
    }

    @Override
    protected void init() {
        int panelHeight = Math.min(panel.preferredHeight(), height - MARGIN * 2);
        panel.setBounds((width - WIDTH) / 2, (height - panelHeight) / 2, WIDTH, panelHeight);
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return panel.mouseScrolled(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private static final class MenuPanel extends OverlayPanel {
        private static final int ACTION_HEIGHT = 16;
        private static final int ACTION_COLUMNS = 3;
        private static final int ROW_HEIGHT = 15;
        private static final int GROUP_HEIGHT = 14;
        private static final int TOGGLE_WIDTH = 52;
        private static final String[] GROUPS = {"ah", "sell", "chests", "shop", "tools", "system"};
        private static final Map<String, String> GROUP_OF = Map.ofEntries(
                Map.entry("ahsearch", "ah"), Map.entry("myauctions", "ah"), Map.entry("alerts", "ah"),
                Map.entry("sellassist", "sell"), Map.entry("orders", "sell"),
                Map.entry("containervalue", "chests"), Map.entry("farm", "chests"), Map.entry("tooltips", "chests"),
                Map.entry("sellstats", "shop"), Map.entry("minutetest", "shop"),
                Map.entry("sync", "system"), Map.entry("monitor", "system"), Map.entry("dumper", "system"));

        private final ModuleRegistry registry;
        private final List<Chip> actionChips = new ArrayList<>();
        private final Map<String, List<Module>> groups = new LinkedHashMap<>();
        private final Map<String, Label> groupLabels = new LinkedHashMap<>();
        private final Map<String, Label> names = new LinkedHashMap<>();
        private final Map<String, Chip> toggles = new LinkedHashMap<>();
        private final Label hint;
        private int listTop;
        private int listHeight;
        private int listContentHeight;
        private int scroll;

        MenuPanel(ModuleRegistry registry, MenuActions actions) {
            this.registry = registry;
            setTitle(I18n.get("hugomarket.menu.title"));
            setHeaderRight(() -> "v" + HugoMarket.modVersion().split("\\+")[0]);
            for (MenuActions.Action action : actions.all()) {
                actionChips.add(add(new Chip(I18n.get(action.labelKey()), () -> false, action.run())));
            }
            for (String group : GROUPS) {
                groups.put(group, new ArrayList<>());
            }
            for (Module module : registry.modules()) {
                groups.get(GROUP_OF.getOrDefault(module.id(), "tools")).add(module);
            }
            for (String group : GROUPS) {
                if (groups.get(group).isEmpty()) {
                    continue;
                }
                groupLabels.put(group, add(new Label(() -> I18n.get("hugomarket.menu.group." + group), Theme.ACCENT)));
                for (Module module : groups.get(group)) {
                    names.put(module.id(), add(new Label(() -> I18n.get("hugomarket.module." + module.id()), Theme.TEXT)));
                    toggles.put(module.id(), add(new Chip(() -> toggleLabel(module.id()), () -> registry.isEnabled(module.id()), () -> toggle(module))));
                }
            }
            hint = add(new Label(() -> I18n.get("hugomarket.menu.hint")));
        }

        private String toggleLabel(String id) {
            if (!registry.isEnabled(id)) {
                return I18n.get("hugomarket.menu.off");
            }
            return registry.isRunning(id) ? I18n.get("hugomarket.menu.on") : I18n.get("hugomarket.menu.restart");
        }

        int preferredHeight() {
            int actionRows = (actionChips.size() + ACTION_COLUMNS - 1) / ACTION_COLUMNS;
            return Theme.PADDING * 2 + headerHeight() + actionRows * (ACTION_HEIGHT + Theme.GAP) + Theme.GAP + contentHeight() + Theme.GAP + Theme.LINE;
        }

        private int contentHeight() {
            int total = 0;
            for (String group : groupLabels.keySet()) {
                total += GROUP_HEIGHT + groups.get(group).size() * ROW_HEIGHT + Theme.GAP;
            }
            return total;
        }

        private void toggle(Module module) {
            registry.setEnabled(module.id(), !registry.isEnabled(module.id()));
        }

        @Override
        protected void layout() {
            int innerX = x + Theme.PADDING;
            int innerWidth = width - Theme.PADDING * 2;
            int cursorY = contentTop();
            int actionWidth = (innerWidth - Theme.GAP * (ACTION_COLUMNS - 1)) / ACTION_COLUMNS;
            for (int i = 0; i < actionChips.size(); i++) {
                actionChips.get(i).setBounds(innerX + (i % ACTION_COLUMNS) * (actionWidth + Theme.GAP),
                        cursorY + (i / ACTION_COLUMNS) * (ACTION_HEIGHT + Theme.GAP), actionWidth, ACTION_HEIGHT);
            }
            cursorY += ((actionChips.size() + ACTION_COLUMNS - 1) / ACTION_COLUMNS) * (ACTION_HEIGHT + Theme.GAP) + Theme.GAP;
            listTop = cursorY;
            listHeight = Math.max(0, y + height - Theme.PADDING - Theme.LINE - Theme.GAP - listTop);
            listContentHeight = contentHeight();
            scroll = Math.max(0, Math.min(scroll, listContentHeight - listHeight));
            int rowY = listTop - scroll;
            for (Map.Entry<String, Label> entry : groupLabels.entrySet()) {
                entry.getValue().setBounds(innerX, rowY, innerWidth, GROUP_HEIGHT);
                rowY += GROUP_HEIGHT;
                for (Module module : groups.get(entry.getKey())) {
                    names.get(module.id()).setBounds(innerX + 4, rowY, innerWidth - TOGGLE_WIDTH - 8, ROW_HEIGHT);
                    toggles.get(module.id()).setBounds(innerX + innerWidth - TOGGLE_WIDTH, rowY + 1, TOGGLE_WIDTH, ROW_HEIGHT - 3);
                    rowY += ROW_HEIGHT;
                }
                rowY += Theme.GAP;
            }
            hint.setBounds(innerX, y + height - Theme.PADDING - Theme.LINE, innerWidth, Theme.LINE);
        }

        @Override
        public void render(Gfx gfx, int mouseX, int mouseY) {
            renderFrame(gfx);
            for (Chip chip : actionChips) {
                chip.render(gfx, mouseX, mouseY);
            }
            gfx.fill(x + Theme.PADDING, listTop - 1, width - Theme.PADDING * 2, listHeight + 2, Theme.FIELD_BG);
            gfx.scissor(x + Theme.PADDING, listTop, width - Theme.PADDING * 2, listHeight);
            int inListY = inList(mouseY) ? mouseY : -1;
            for (Label label : groupLabels.values()) {
                label.render(gfx, mouseX, inListY);
            }
            for (Label label : names.values()) {
                label.render(gfx, mouseX, inListY);
            }
            for (Chip chip : toggles.values()) {
                chip.render(gfx, mouseX, inListY);
            }
            gfx.unscissor();
            if (listContentHeight > listHeight) {
                int barHeight = Math.max(8, listHeight * listHeight / listContentHeight);
                int barY = listTop + (listHeight - barHeight) * scroll / (listContentHeight - listHeight);
                gfx.fill(x + width - Theme.PADDING - 2, barY, 2, barHeight, Theme.TEXT_DIM);
            }
            hint.render(gfx, mouseX, mouseY);
        }

        private boolean inList(double mouseY) {
            return mouseY >= listTop && mouseY < listTop + listHeight;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!contains(mouseX, mouseY)) {
                return false;
            }
            for (Chip chip : actionChips) {
                if (chip.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            if (inList(mouseY)) {
                for (Chip chip : toggles.values()) {
                    if (chip.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (!contains(mouseX, mouseY) || listContentHeight <= listHeight) {
                return contains(mouseX, mouseY);
            }
            scroll = Math.max(0, Math.min(scroll - (int) (delta * ROW_HEIGHT), listContentHeight - listHeight));
            layout();
            return true;
        }
    }
}
