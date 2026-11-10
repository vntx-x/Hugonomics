package dev.hugomarket.module.watchlist;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.Money;
import dev.hugomarket.module.ahsearch.AhJump;
import dev.hugomarket.module.ahsearch.Deals;
import dev.hugomarket.module.ahsearch.SearchQuery;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.UserData;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.ItemLabel;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.ListView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

final class WatchlistScreen extends Screen {
    private static final int WIDTH = 380;
    private static final int MARGIN = 16;

    private final Screen parent;
    private final WatchlistPanel panel;
    private final Runnable refresh;
    private int ticks;

    WatchlistScreen(Screen parent, MarketSession market, UserData userData, Runnable refresh) {
        super(Component.translatable("hugomarket.watchlist.title"));
        this.parent = parent;
        this.refresh = refresh;
        this.panel = new WatchlistPanel(market, userData);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(WIDTH, width - MARGIN * 2);
        panel.setBounds((width - panelWidth) / 2, MARGIN, panelWidth, height - MARGIN * 2);
    }

    @Override
    public void tick() {
        if (++ticks % 100 == 0) {
            refresh.run();
        }
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

    private static final class WatchlistPanel extends OverlayPanel {
        private static final int ICON_SIZE = 16;

        private final MarketSession market;
        private final UserData userData;
        private final ListView<WatchlistRows.Row> list;
        private final Map<String, ItemStack> icons = new HashMap<>();
        private final Map<String, String> names = new HashMap<>();
        private int version = -1;

        WatchlistPanel(MarketSession market, UserData userData) {
            this.market = market;
            this.userData = userData;
            setTitle(I18n.get("hugomarket.watchlist.title"));
            list = add(new ListView<>(Theme.ROW_HEIGHT, this::renderRow, this::clicked));
            setHeaderRight(() -> I18n.get("hugomarket.watchlist.count", list.rowCount()));
            list.setHoverRenderer((gfx, row, mouseX, mouseY) -> {
                ItemStack stack = icon(row);
                if (!stack.isEmpty()) {
                    gfx.itemTooltip(stack, mouseX, mouseY);
                }
            });
            rebuild();
        }

        private void clicked(WatchlistRows.Row row, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                userData.toggleWatch(row.key());
                rebuild();
            } else if (row.hasOffers()) {
                AhJump.open(market, row.cheapest());
            }
        }

        private void rebuild() {
            version = market.version();
            list.setRows(WatchlistRows.build(userData.watchlist(), market.index().query(SearchQuery.EMPTY), market::stats));
        }

        private ItemStack icon(WatchlistRows.Row row) {
            String id = row.hasOffers() ? row.cheapest().listing().item().id() : ItemKey.parse(row.key()).base();
            return icons.computeIfAbsent(id, unused -> {
                ResourceLocation location = ResourceLocation.tryParse(id);
                Item item = location == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(location);
                return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
            });
        }

        private String name(WatchlistRows.Row row) {
            ItemKey key = ItemKey.parse(row.key());
            if (row.hasOffers()) {
                return ItemLabel.of(key, row.cheapest().listing().item().displayName());
            }
            return names.computeIfAbsent(row.key(), unused -> {
                String base = key.base();
                for (String prefix : new String[] {"item.minecraft.", "block.minecraft."}) {
                    if (I18n.exists(prefix + base)) {
                        return ItemLabel.of(key, I18n.get(prefix + base));
                    }
                }
                int colon = base.indexOf(':');
                return ItemLabel.of(key, (colon < 0 ? base : base.substring(colon + 1)).replace('_', ' ').replace('-', ' '));
            });
        }

        @Override
        protected void layout() {
            int innerX = x + Theme.PADDING;
            int innerWidth = width - Theme.PADDING * 2;
            int top = contentTop();
            list.setBounds(innerX, top, innerWidth, y + height - Theme.PADDING - Theme.LINE - Theme.GAP - top);
        }

        @Override
        public void render(Gfx gfx, int mouseX, int mouseY) {
            if (market.version() != version) {
                rebuild();
            }
            super.render(gfx, mouseX, mouseY);
            String hint = list.rowCount() == 0 ? I18n.get("hugomarket.watchlist.empty") : I18n.get("hugomarket.watchlist.hint");
            gfx.textNoShadow(gfx.clip(hint, width - Theme.PADDING * 2), x + Theme.PADDING, y + height - Theme.PADDING - Theme.LINE + 2, Theme.TEXT_DIM);
        }

        private void renderRow(Gfx gfx, WatchlistRows.Row row, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
            ItemStack stack = icon(row);
            if (!stack.isEmpty()) {
                gfx.item(stack, rowX + 3, rowY + (rowHeight - ICON_SIZE) / 2);
            }
            boolean bargain = row.ratio().isPresent() && Deals.isBargain(row.ratio().getAsDouble());
            if (bargain) {
                int mark = Deals.isSuspicious(row.ratio().getAsDouble()) ? Theme.WATCH_FRAME : Theme.BARGAIN_FRAME;
                gfx.fill(rowX, rowY + 1, 2, rowHeight - 2, mark);
            }
            String median = row.stats() == null ? "" : I18n.get("hugomarket.watchlist.median", Money.compact(row.stats().medianUnitCents()));
            String delta = row.ratio().isPresent() ? Deals.deltaText(row.ratio().getAsDouble()) : "";
            int rightWidth = Math.max(gfx.textWidth(median), gfx.textWidth(delta));
            int textX = rowX + ICON_SIZE + 6;
            int nameWidth = rowWidth - ICON_SIZE - 12 - rightWidth;
            String detail = row.hasOffers()
                    ? I18n.get("hugomarket.watchlist.row", row.offers(), Money.format(row.cheapest().listing().unitPriceCents()))
                    : I18n.get("hugomarket.watchlist.none");
            gfx.textNoShadow(gfx.clip(name(row), nameWidth), textX, rowY + 2, row.hasOffers() ? Theme.TEXT : Theme.TEXT_DIM);
            gfx.textNoShadow(gfx.clip(detail, nameWidth), textX, rowY + 11, Theme.TEXT_DIM);
            gfx.textNoShadow(median, rowX + rowWidth - gfx.textWidth(median) - 3, rowY + 2, Theme.PRICE);
            if (!delta.isEmpty()) {
                int deltaColor = bargain ? Theme.BARGAIN_FRAME : Theme.TEXT_DIM;
                gfx.textNoShadow(delta, rowX + rowWidth - gfx.textWidth(delta) - 3, rowY + 11, deltaColor);
            }
        }
    }
}
