package dev.hugomarket.config;

import dev.hugomarket.HugoMarket;
import dev.hugomarket.core.MenuActions;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleRegistry;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
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
    private static final int WIDTH = 320;
    private static final int MARGIN = 10;

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
        private static final int BANNER_HEIGHT = 30;
        private static final int TILE_HEIGHT = 26;
        private static final int TILE_COLUMNS = 2;
        private static final int ROW_HEIGHT = 22;
        private static final int GROUP_HEIGHT = 15;
        private static final int TOGGLE_WIDTH = 40;
        private static final float BANNER_SCALE = 1.5f;
        private static final String[] GROUPS = {"ah", "sell", "chests", "shop", "tools", "system"};
        private static final Map<String, String> GROUP_OF = Map.ofEntries(
                Map.entry("ahsearch", "ah"), Map.entry("myauctions", "ah"), Map.entry("alerts", "ah"),
                Map.entry("sellassist", "sell"), Map.entry("orders", "sell"),
                Map.entry("containervalue", "chests"), Map.entry("farm", "chests"), Map.entry("tooltips", "chests"),
                Map.entry("sellstats", "shop"), Map.entry("minutetest", "shop"),
                Map.entry("sync", "system"), Map.entry("monitor", "system"), Map.entry("dumper", "system"));

        private record Tile(Chip chip, String title, String description) {
        }

        private record Row(Module module, Chip toggle, String title, String description) {
        }

        private final ModuleRegistry registry;
        private final List<Tile> tiles = new ArrayList<>();
        private final Map<String, List<Row>> groups = new LinkedHashMap<>();
        private final Map<String, Integer> groupY = new LinkedHashMap<>();
        private int listTop;
        private int listHeight;
        private int listContentHeight;
        private int scroll;

        MenuPanel(ModuleRegistry registry, MenuActions actions) {
            this.registry = registry;
            setTitle(I18n.get("hugomarket.menu.title"));
            setHeaderRight(() -> "v" + HugoMarket.modVersion().split("\\+")[0]);
            for (MenuActions.Action action : actions.all()) {
                String description = I18n.exists(action.labelKey() + ".desc") ? I18n.get(action.labelKey() + ".desc") : "";
                tiles.add(new Tile(add(new Chip("", () -> false, action.run())), I18n.get(action.labelKey()), description));
            }
            for (String group : GROUPS) {
                groups.put(group, new ArrayList<>());
            }
            for (Module module : registry.modules()) {
                String descKey = "hugomarket.module." + module.id() + ".desc";
                groups.get(GROUP_OF.getOrDefault(module.id(), "tools")).add(new Row(module,
                        add(new Chip(() -> toggleLabel(module.id()), () -> registry.isEnabled(module.id()), () -> toggle(module))),
                        I18n.get("hugomarket.module." + module.id()), I18n.exists(descKey) ? I18n.get(descKey) : ""));
            }
            groups.values().removeIf(List::isEmpty);
        }

        private String toggleLabel(String id) {
            if (!registry.isEnabled(id)) {
                return I18n.get("hugomarket.menu.off");
            }
            return registry.isRunning(id) ? I18n.get("hugomarket.menu.on") : I18n.get("hugomarket.menu.restart");
        }

        int preferredHeight() {
            return Theme.PADDING * 2 + headerHeight() + BANNER_HEIGHT + tileRows() * (TILE_HEIGHT + Theme.GAP) + Theme.GAP
                    + contentHeight() + Theme.GAP + Theme.LINE;
        }

        private int tileRows() {
            return (tiles.size() + TILE_COLUMNS - 1) / TILE_COLUMNS;
        }

        private int contentHeight() {
            int total = 0;
            for (List<Row> rows : groups.values()) {
                total += GROUP_HEIGHT + rows.size() * ROW_HEIGHT + Theme.GAP;
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
            int cursorY = contentTop() + BANNER_HEIGHT;
            int tileWidth = (innerWidth - Theme.GAP * (TILE_COLUMNS - 1)) / TILE_COLUMNS;
            for (int i = 0; i < tiles.size(); i++) {
                tiles.get(i).chip().setBounds(innerX + (i % TILE_COLUMNS) * (tileWidth + Theme.GAP),
                        cursorY + (i / TILE_COLUMNS) * (TILE_HEIGHT + Theme.GAP), tileWidth, TILE_HEIGHT);
            }
            cursorY += tileRows() * (TILE_HEIGHT + Theme.GAP) + Theme.GAP;
            listTop = cursorY;
            listHeight = Math.max(0, y + height - Theme.PADDING - Theme.LINE - Theme.GAP - listTop);
            listContentHeight = contentHeight();
            scroll = Math.max(0, Math.min(scroll, listContentHeight - listHeight));
            int rowY = listTop - scroll;
            for (Map.Entry<String, List<Row>> entry : groups.entrySet()) {
                groupY.put(entry.getKey(), rowY);
                rowY += GROUP_HEIGHT;
                for (Row row : entry.getValue()) {
                    row.toggle().setBounds(innerX + innerWidth - TOGGLE_WIDTH - 4, rowY + (ROW_HEIGHT - Theme.CHIP_HEIGHT) / 2, TOGGLE_WIDTH, Theme.CHIP_HEIGHT);
                    rowY += ROW_HEIGHT;
                }
                rowY += Theme.GAP;
            }
        }

        @Override
        public void render(Gfx gfx, int mouseX, int mouseY) {
            renderFrame(gfx);
            int innerX = x + Theme.PADDING;
            int innerWidth = width - Theme.PADDING * 2;
            int bannerY = contentTop();
            gfx.fill(innerX, bannerY, innerWidth, BANNER_HEIGHT - Theme.GAP, Theme.HEADER_BG);
            gfx.fill(innerX, bannerY, 3, BANNER_HEIGHT - Theme.GAP, Theme.ACCENT);
            gfx.textScaled(I18n.get("hugomarket.menu.title"), innerX + 8, bannerY + 4, BANNER_SCALE, Theme.ACCENT);
            gfx.textNoShadow(I18n.get("hugomarket.menu.tagline"), innerX + 8, bannerY + 18, Theme.TEXT_DIM);
            for (Tile tile : tiles) {
                Chip chip = tile.chip();
                chip.render(gfx, mouseX, mouseY);
                gfx.textNoShadow(gfx.clip(tile.title(), chip.width() - 8), chip.x() + 4, chip.y() + 4, Theme.TEXT);
                gfx.textNoShadow(gfx.clip(tile.description(), chip.width() - 8), chip.x() + 4, chip.y() + 15, Theme.TEXT_DIM);
            }
            gfx.fill(innerX, listTop - 1, innerWidth, listHeight + 2, Theme.FIELD_BG);
            gfx.scissor(innerX, listTop, innerWidth, listHeight);
            int inListY = inList(mouseY) ? mouseY : -1;
            for (Map.Entry<String, List<Row>> entry : groups.entrySet()) {
                int gy = groupY.getOrDefault(entry.getKey(), listTop);
                gfx.fill(innerX, gy + 2, innerWidth, GROUP_HEIGHT - 4, Theme.HEADER_BG);
                gfx.fill(innerX, gy + 2, 2, GROUP_HEIGHT - 4, Theme.ACCENT);
                gfx.textNoShadow(I18n.get("hugomarket.menu.group." + entry.getKey()), innerX + 6, gy + 4, Theme.ACCENT);
                int rowY = gy + GROUP_HEIGHT;
                for (Row row : entry.getValue()) {
                    boolean on = registry.isEnabled(row.module().id());
                    int textWidth = innerWidth - TOGGLE_WIDTH - 14;
                    gfx.textNoShadow(gfx.clip(row.title(), textWidth), innerX + 6, rowY + 2, on ? Theme.TEXT : Theme.TEXT_DIM);
                    gfx.textNoShadow(gfx.clip(row.description(), textWidth), innerX + 6, rowY + 12, Theme.TEXT_PLACEHOLDER);
                    row.toggle().render(gfx, mouseX, inListY);
                    rowY += ROW_HEIGHT;
                }
            }
            gfx.unscissor();
            if (listContentHeight > listHeight) {
                int barHeight = Math.max(8, listHeight * listHeight / listContentHeight);
                int barY = listTop + (listHeight - barHeight) * scroll / (listContentHeight - listHeight);
                gfx.fill(x + width - Theme.PADDING - 2, barY, 2, barHeight, Theme.TEXT_DIM);
            }
            gfx.textNoShadow(I18n.get("hugomarket.menu.hint"), innerX, y + height - Theme.PADDING - Theme.LINE + 2, Theme.TEXT_DIM);
        }

        private boolean inList(double mouseY) {
            return mouseY >= listTop && mouseY < listTop + listHeight;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!contains(mouseX, mouseY)) {
                return false;
            }
            for (Tile tile : tiles) {
                if (tile.chip().mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            if (inList(mouseY)) {
                for (List<Row> rows : groups.values()) {
                    for (Row row : rows) {
                        if (row.toggle().mouseClicked(mouseX, mouseY, button)) {
                            return true;
                        }
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
