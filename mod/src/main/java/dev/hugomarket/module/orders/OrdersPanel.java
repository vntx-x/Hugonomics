package dev.hugomarket.module.orders;

import dev.hugomarket.mixin.AbstractContainerScreenAccessor;
import dev.hugomarket.model.Money;
import dev.hugomarket.model.Order;
import dev.hugomarket.store.FlipMath;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.OrderBook;
import dev.hugomarket.store.PriceStats;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Label;
import dev.hugomarket.ui.widget.ListView;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class OrdersPanel extends OverlayPanel {
    record Row(Order order, PriceStats ah, FlipMath.Flip flip, ItemStack icon) {
    }

    private static final int ICON_SIZE = 16;
    private static final int SLOT_SIZE = 16;
    private static final int FLIP_FRAME = 0xFF55DDFF;

    private final AbstractContainerScreen<?> screen;
    private final MarketSession market;
    private final Label title;
    private final Label status;
    private final ListView<Row> list;
    private final BitSet profitable = new BitSet();
    private List<Row> rows = List.of();

    OrdersPanel(AbstractContainerScreen<?> screen, MarketSession market) {
        this.screen = screen;
        this.market = market;
        title = add(new Label(() -> I18n.get("hugomarket.orders.title"), Theme.ACCENT));
        status = add(new Label(this::statusText));
        list = add(new ListView<>(Theme.ROW_HEIGHT, this::renderRow, (row, button) -> {
        }));
        list.setHoverRenderer((gfx, row, mouseX, mouseY) -> {
            if (!row.icon().isEmpty()) {
                gfx.itemTooltip(row.icon(), mouseX, mouseY);
            }
        });
    }

    void update(List<Order> orders, Map<Integer, ItemStack> icons) {
        List<Row> fresh = new ArrayList<>(orders.size());
        profitable.clear();
        for (Order order : orders) {
            PriceStats ah = market.stats(order.key());
            OrderBook.BestOrder single = new OrderBook.BestOrder(order.key().toString(), order.unitPriceCents(), order.open(), order.owner(), 0);
            FlipMath.Flip flip = FlipMath.ahToOrder(ah, single, (int) Math.min(order.open(), 64));
            if (flip != null && flip.marginUnitCents() > 0) {
                profitable.set(order.slot());
            }
            fresh.add(new Row(order, ah, flip, icons.getOrDefault(order.slot(), ItemStack.EMPTY)));
        }
        fresh.sort(Comparator.comparingLong((Row r) -> r.flip() == null ? Long.MIN_VALUE : r.flip().marginUnitCents()).reversed());
        rows = fresh;
        list.setRows(rows);
    }

    @Override
    protected void layout() {
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        int cursorY = y + Theme.PADDING;
        title.setBounds(innerX, cursorY, innerWidth, 10);
        cursorY += 10 + Theme.GAP;
        status.setBounds(innerX, cursorY, innerWidth, 10);
        cursorY += 10 + Theme.GAP;
        list.setBounds(innerX, cursorY, innerWidth, y + height - Theme.PADDING - cursorY);
    }

    private String statusText() {
        long withPrice = rows.stream().filter(r -> r.ah() != null).count();
        return I18n.get("hugomarket.orders.status", rows.size(), withPrice, profitable.cardinality());
    }

    private void renderRow(Gfx gfx, Row row, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
        if (!row.icon().isEmpty()) {
            gfx.item(row.icon(), rowX + 1, rowY + (rowHeight - ICON_SIZE) / 2);
        }
        int textX = rowX + ICON_SIZE + 4;
        String right = row.flip() == null ? "?" : (row.flip().marginUnitCents() >= 0 ? "+" : "") + Money.compact(row.flip().marginUnitCents());
        int rightColor = row.flip() == null ? Theme.TEXT_DIM : row.flip().marginUnitCents() > 0 ? Theme.PRICE : 0xFFFF6B6B;
        int rightWidth = gfx.textWidth(right);
        int nameWidth = rowWidth - ICON_SIZE - 8 - rightWidth;
        gfx.textNoShadow(gfx.clip(row.order().item().displayName(), nameWidth), textX, rowY + 2, Theme.TEXT);
        String detail = I18n.get("hugomarket.orders.row", Money.compact(row.order().unitPriceCents()), row.order().open(),
                row.ah() == null ? "–" : Money.compact(row.ah().lowestUnitCents()));
        gfx.textNoShadow(gfx.clip(detail, nameWidth), textX, rowY + 11, Theme.TEXT_DIM);
        gfx.textNoShadow(right, rowX + rowWidth - rightWidth - 3, rowY + 6, rightColor);
    }

    @Override
    public void renderBehind(Gfx gfx, int mouseX, int mouseY) {
        if (profitable.isEmpty()) {
            return;
        }
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int left = accessor.hugomarket$leftPos();
        int top = accessor.hugomarket$topPos();
        for (Slot slot : screen.getMenu().slots) {
            if (profitable.get(slot.index)) {
                gfx.border(left + slot.x - 1, top + slot.y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, FLIP_FRAME);
            }
        }
    }
}
