package dev.hugomarket.module.spawners;

import dev.hugomarket.model.Money;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.ListView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

final class SpawnersScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int MARGIN = 16;

    private final Screen parent;
    private final SpawnersPanel panel;

    SpawnersScreen(Screen parent, MarketSession market, boolean online) {
        super(Component.translatable("hugomarket.spawners.title"));
        this.parent = parent;
        this.panel = new SpawnersPanel(market, online);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(WIDTH, width - MARGIN * 2);
        panel.setBounds((width - panelWidth) / 2, MARGIN, panelWidth, height - MARGIN * 2);
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

    private static final class SpawnersPanel extends OverlayPanel {
        private static final int ICON_SIZE = 16;

        private final MarketSession market;
        private final boolean online;
        private final ListView<SpawnerRows.Row> list;
        private final ItemStack icon = new ItemStack(Items.SPAWNER);
        private final Map<String, String> names = new HashMap<>();
        private final String perPiece = I18n.get("hugomarket.spawners.per_piece");
        private int version = -1;
        private String hint = "";

        SpawnersPanel(MarketSession market, boolean online) {
            this.market = market;
            this.online = online;
            setTitle(I18n.get("hugomarket.spawners.title"));
            list = add(new ListView<>(Theme.ROW_HEIGHT, this::renderRow, (row, button) -> { }));
            setHeaderRight(() -> I18n.get("hugomarket.spawners.count", list.rowCount()));
            rebuild();
        }

        private void rebuild() {
            version = market.version();
            list.setRows(SpawnerRows.build(market.spawnerPrices()));
            if (!online) {
                hint = I18n.get("hugomarket.spawners.offline");
            } else if (market.spawnerPrices().isEmpty()) {
                hint = I18n.get("hugomarket.spawners.empty");
            } else {
                hint = I18n.get("hugomarket.spawners.hint", market.spawnerPrices().updatedAt());
            }
        }

        private String name(String type) {
            return names.computeIfAbsent(type, unused -> {
                String entityKey = "entity.minecraft." + type;
                String entity = I18n.exists(entityKey) ? I18n.get(entityKey) : type.replace('_', ' ');
                return I18n.get("hugomarket.spawners.name", entity);
            });
        }

        @Override
        protected void layout() {
            int top = contentTop();
            int bottom = y + height - Theme.PADDING - Theme.LINE - Theme.GAP;
            list.setBounds(x + Theme.PADDING, top, width - Theme.PADDING * 2, bottom - top);
        }

        @Override
        public void render(Gfx gfx, int mouseX, int mouseY) {
            if (market.version() != version) {
                rebuild();
            }
            super.render(gfx, mouseX, mouseY);
            gfx.textNoShadow(gfx.clip(hint, width - Theme.PADDING * 2), x + Theme.PADDING, y + height - Theme.PADDING - Theme.LINE + 2, Theme.TEXT_DIM);
        }

        private void renderRow(Gfx gfx, SpawnerRows.Row row, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
            gfx.item(icon, rowX + 3, rowY + (rowHeight - ICON_SIZE) / 2);
            String price = Money.format(row.unitCents());
            int priceWidth = Math.max(gfx.textWidth(price), gfx.textWidth(perPiece));
            int textX = rowX + ICON_SIZE + 6;
            gfx.textNoShadow(gfx.clip(name(row.type()), rowWidth - ICON_SIZE - 12 - priceWidth), textX, rowY + (rowHeight - 8) / 2, Theme.TEXT);
            gfx.textNoShadow(price, rowX + rowWidth - gfx.textWidth(price) - 3, rowY + 2, Theme.PRICE);
            gfx.textNoShadow(perPiece, rowX + rowWidth - gfx.textWidth(perPiece) - 3, rowY + 11, Theme.TEXT_DIM);
        }
    }
}
