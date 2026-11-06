package dev.hugomarket.module.deals;

import dev.hugomarket.model.Money;
import dev.hugomarket.module.ahsearch.AhJump;
import dev.hugomarket.module.ahsearch.Deals;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.module.ahsearch.SearchQuery;
import dev.hugomarket.store.Fees;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.ui.ItemLabel;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.ListView;
import dev.hugomarket.ui.widget.TextField;
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
import java.util.List;
import java.util.Map;

//? if >=1.21.9 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

final class DealsScreen extends Screen {
    private static final int WIDTH = 380;
    private static final int MARGIN = 16;

    private final Screen parent;
    private final DealsPanel panel;
    private final Runnable refresh;
    private int ticks;

    DealsScreen(Screen parent, MarketSession market, Fees fees, Runnable refresh) {
        super(Component.translatable("hugomarket.deals.title"));
        this.parent = parent;
        this.refresh = refresh;
        this.panel = new DealsPanel(market, fees);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(WIDTH, width - MARGIN * 2);
        panel.setBounds((width - panelWidth) / 2, MARGIN, panelWidth, height - MARGIN * 2);
        panel.focus();
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

    private boolean handleKey(int key, int scancode, int modifiers) {
        if (panel.keyPressed(key, scancode, modifiers)) {
            return true;
        }
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
        return handleKey(event.key(), event.scancode(), event.modifiers()) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return (Character.isBmpCodePoint(event.codepoint()) && panel.charTyped((char) event.codepoint())) || super.charTyped(event);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return handleKey(key, scancode, modifiers) || super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return panel.charTyped(character) || super.charTyped(character, modifiers);
    }
    //?}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return panel.mouseScrolled(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private static final class DealsPanel extends OverlayPanel {
        private static final int ICON_SIZE = 16;

        private final MarketSession market;
        private final Fees fees;
        private final TextField search;
        private final Chip byProfit;
        private final Chip byDiscount;
        private final ListView<DealFinder.Deal> list;
        private final Map<String, ItemStack> icons = new HashMap<>();
        private DealFinder.Order order = DealFinder.Order.PROFIT;
        private String filter = "";
        private int version = -1;

        DealsPanel(MarketSession market, Fees fees) {
            this.market = market;
            this.fees = fees;
            setTitle(I18n.get("hugomarket.deals.title"));
            search = add(new TextField(I18n.get("hugomarket.deals.search"), "", text -> {
                filter = text;
                rebuild();
            }));
            byProfit = add(new Chip(I18n.get("hugomarket.deals.by_profit"), () -> order == DealFinder.Order.PROFIT, () -> setOrder(DealFinder.Order.PROFIT)));
            byDiscount = add(new Chip(I18n.get("hugomarket.deals.by_discount"), () -> order == DealFinder.Order.DISCOUNT, () -> setOrder(DealFinder.Order.DISCOUNT)));
            list = add(new ListView<>(Theme.ROW_HEIGHT, this::renderRow,
                    (deal, button) -> AhJump.open(market, deal.entry().listing())));
            setHeaderRight(() -> I18n.get("hugomarket.deals.count", list.rowCount(), market.size()));
            list.setHoverRenderer((gfx, deal, mouseX, mouseY) -> {
                ItemStack stack = icon(deal.entry());
                if (!stack.isEmpty()) {
                    gfx.itemTooltip(stack, mouseX, mouseY);
                }
            });
            rebuild();
        }

        void focus() {
            search.setFocused(true);
        }

        private void setOrder(DealFinder.Order newOrder) {
            order = newOrder;
            rebuild();
        }

        private void rebuild() {
            version = market.version();
            List<ListingIndex.Entry> entries = market.index().query(SearchQuery.EMPTY);
            list.setRows(DealFinder.find(entries, market::stats, fees, filter, order));
        }

        private ItemStack icon(ListingIndex.Entry entry) {
            return icons.computeIfAbsent(entry.listing().item().id() + "x" + entry.listing().item().count(), unused -> {
                ResourceLocation id = ResourceLocation.tryParse(entry.listing().item().id());
                Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
                return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, entry.listing().item().count());
            });
        }

        @Override
        protected void layout() {
            int innerX = x + Theme.PADDING;
            int innerWidth = width - Theme.PADDING * 2;
            int cursorY = contentTop();
            int chipWidth = 64;
            search.setBounds(innerX, cursorY, innerWidth - (chipWidth + Theme.GAP) * 2, Theme.FIELD_HEIGHT);
            byProfit.setBounds(innerX + innerWidth - chipWidth * 2 - Theme.GAP, cursorY + 1, chipWidth, Theme.CHIP_HEIGHT);
            byDiscount.setBounds(innerX + innerWidth - chipWidth, cursorY + 1, chipWidth, Theme.CHIP_HEIGHT);
            cursorY += Theme.FIELD_HEIGHT + Theme.GAP;
            list.setBounds(innerX, cursorY, innerWidth, y + height - Theme.PADDING - Theme.LINE - Theme.GAP - cursorY);
        }

        @Override
        public void render(Gfx gfx, int mouseX, int mouseY) {
            if (market.version() != version) {
                rebuild();
            }
            super.render(gfx, mouseX, mouseY);
            String hint = list.rowCount() == 0 ? I18n.get("hugomarket.deals.empty") : I18n.get("hugomarket.deals.hint");
            gfx.textNoShadow(gfx.clip(hint, width - Theme.PADDING * 2), x + Theme.PADDING, y + height - Theme.PADDING - Theme.LINE + 2, Theme.TEXT_DIM);
        }

        private void renderRow(Gfx gfx, DealFinder.Deal deal, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
            ListingIndex.Entry entry = deal.entry();
            ItemStack stack = icon(entry);
            if (!stack.isEmpty()) {
                gfx.item(stack, rowX + 3, rowY + (rowHeight - ICON_SIZE) / 2);
            }
            int mark = Deals.isSuspicious(deal.ratio()) ? Theme.WATCH_FRAME : Theme.BARGAIN_FRAME;
            gfx.fill(rowX, rowY + 1, 2, rowHeight - 2, mark);
            String profit = "+" + Money.compact(deal.profitCents());
            String discount = Deals.deltaText(deal.ratio());
            int rightWidth = Math.max(gfx.textWidth(profit), gfx.textWidth(discount));
            int textX = rowX + ICON_SIZE + 6;
            int nameWidth = rowWidth - ICON_SIZE - 12 - rightWidth;
            String name = (entry.listing().item().count() > 1 ? entry.listing().item().count() + "x " : "") + ItemLabel.of(entry.listing().key(), entry.listing().item().displayName());
            String detail = I18n.get("hugomarket.deals.row", Money.format(entry.listing().priceCents()),
                    Money.format(entry.listing().unitPriceCents()), ageMinutes(entry));
            gfx.textNoShadow(gfx.clip(name, nameWidth), textX, rowY + 2, Theme.TEXT);
            gfx.textNoShadow(gfx.clip(detail, nameWidth), textX, rowY + 11, Theme.TEXT_DIM);
            gfx.textNoShadow(profit, rowX + rowWidth - gfx.textWidth(profit) - 3, rowY + 2, Theme.PRICE);
            gfx.textNoShadow(discount, rowX + rowWidth - gfx.textWidth(discount) - 3, rowY + 11, mark);
        }

        private static long ageMinutes(ListingIndex.Entry entry) {
            return Math.max(0, (System.currentTimeMillis() - entry.lastSeen()) / 60_000);
        }
    }
}
