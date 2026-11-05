package dev.hugomarket.module.trades;

import dev.hugomarket.model.Money;
import dev.hugomarket.store.AhTotalsTracker;
import dev.hugomarket.store.TradeLog;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Label;
import dev.hugomarket.ui.widget.ListView;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

final class ProfitScreen extends Screen {
    private static final int WIDTH = 360;
    private static final int MARGIN = 20;

    private final Screen parent;
    private final ProfitPanel panel;

    ProfitScreen(Screen parent, TradeLog log) {
        super(Component.translatable("hugomarket.profit.title"));
        this.parent = parent;
        this.panel = new ProfitPanel(log);
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

    //? if >=1.21.9 {
    /*@Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return panel.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scancode, modifiers);
    }
    //?}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return panel.mouseScrolled(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private static final class ProfitPanel extends OverlayPanel {
        private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm");
        private static final long DAY_MS = 24 * 60 * 60_000;

        private final TradeLog log;
        private final List<Label> lines;
        private final ListView<TradeLog.Trade> trades;

        ProfitPanel(TradeLog log) {
            this.log = log;
            long now = System.currentTimeMillis();
            TradeLog.Summary today = log.today(now, ZoneId.systemDefault());
            TradeLog.Summary week = log.since(now - 7 * DAY_MS);
            TradeLog.Summary month = log.since(now - 30 * DAY_MS);
            setTitle(I18n.get("hugomarket.profit.title"));
            lines = List.of(
                    add(new Label(() -> summaryLine("hugomarket.profit.today", today), Theme.TEXT)),
                    add(new Label(() -> summaryLine("hugomarket.profit.week", week), Theme.TEXT)),
                    add(new Label(() -> summaryLine("hugomarket.profit.month", month), Theme.TEXT)),
                    add(new Label(() -> I18n.get("hugomarket.profit.hint"))));
            trades = add(new ListView<>(10, this::renderTrade, (trade, button) -> {
            }));
            trades.setRows(log.recent(200));
        }

        private static String summaryLine(String key, TradeLog.Summary s) {
            return I18n.get(key, Money.compact(s.incomeCents()), Money.compact(s.spentCents()), (s.netCents() >= 0 ? "+" : "") + Money.compact(s.netCents()));
        }

        private void renderTrade(Gfx gfx, TradeLog.Trade trade, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
            String amount = (trade.isIncome() ? "+" : "-") + Money.compact(trade.priceCents());
            int amountWidth = gfx.textWidth(amount);
            String when = TIME.format(Instant.ofEpochMilli(trade.ts()).atZone(ZoneId.systemDefault()));
            String what;
            if (trade.kind() == TradeLog.Kind.SHOP_SELL) {
                what = I18n.get("hugomarket.profit.shop");
            } else if (AhTotalsTracker.KEY.equals(trade.key())) {
                what = I18n.get(trade.kind() == TradeLog.Kind.AH_SELL ? "hugomarket.profit.ah_sales" : "hugomarket.profit.ah_buys");
            } else {
                what = (trade.count() > 1 ? trade.count() + "x " : "") + trade.displayName() + (trade.counterparty().isEmpty() ? "" : " · " + trade.counterparty());
            }
            gfx.textNoShadow(when, rowX + 1, rowY + 1, Theme.TEXT_DIM);
            gfx.textNoShadow(gfx.clip(what, rowWidth - amountWidth - 74), rowX + 68, rowY + 1, Theme.TEXT);
            gfx.textNoShadow(amount, rowX + rowWidth - amountWidth - 3, rowY + 1, trade.isIncome() ? Theme.PRICE : Theme.DANGER);
        }

        @Override
        protected void layout() {
            int innerX = x + Theme.PADDING;
            int innerWidth = width - Theme.PADDING * 2;
            int cursorY = contentTop();
            for (Label label : lines) {
                label.setBounds(innerX, cursorY, innerWidth, Theme.LINE);
                cursorY += Theme.LINE;
            }
            cursorY += Theme.GAP;
            trades.setBounds(innerX, cursorY, innerWidth, y + height - Theme.PADDING - cursorY);
        }
    }
}
