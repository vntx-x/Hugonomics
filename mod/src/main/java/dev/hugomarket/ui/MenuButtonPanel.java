package dev.hugomarket.ui;

import dev.hugomarket.mixin.AbstractContainerScreenAccessor;
import dev.hugomarket.model.Money;
import dev.hugomarket.store.StatusBoard;
import dev.hugomarket.ui.widget.Chip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;

public final class MenuButtonPanel extends OverlayPanel {
    private static final int HEIGHT = 13;
    private static final int GAP_ABOVE = 2;
    private static final int BUTTON_WIDTH = 72;
    private static final int TEXT_PADDING = 5;

    private final Chip button;
    private final StatusBoard status;
    private int version = -1;
    private String today = "";
    private String worth = "";
    private int todayColor = Theme.TEXT_DIM;

    public MenuButtonPanel(AbstractContainerScreen<?> screen, StatusBoard status, Runnable open) {
        this.status = status;
        button = add(new Chip(I18n.get("hugomarket.menu.button"), () -> false, open));
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int left = accessor.hugomarket$leftPos();
        int top = accessor.hugomarket$topPos() + accessor.hugomarket$imageHeight() + GAP_ABOVE;
        setBounds(left, top, BUTTON_WIDTH, HEIGHT);
    }

    @Override
    protected void layout() {
        button.setBounds(x, y, width, height);
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        gfx.fill(x + 1, y + 1, width, height, Theme.PANEL_SHADOW);
        button.render(gfx, mouseX, mouseY);
        gfx.border(x, y, width, height, Theme.PANEL_BORDER);
        if (version != status.version()) {
            refreshTexts();
        }
        if (today.isEmpty() && worth.isEmpty()) {
            return;
        }
        int textY = y + (height - gfx.lineHeight()) / 2 + 1;
        int textX = x + width + TEXT_PADDING;
        int boxWidth = gfx.textWidth(today) + gfx.textWidth(worth) + TEXT_PADDING * 2 + (today.isEmpty() || worth.isEmpty() ? 0 : TEXT_PADDING);
        gfx.fill(textX - 2, y, boxWidth, height, Theme.PANEL_BG);
        gfx.border(textX - 2, y, boxWidth, height, Theme.PANEL_BORDER);
        if (!today.isEmpty()) {
            gfx.textNoShadow(today, textX + 2, textY, todayColor);
            textX += gfx.textWidth(today) + TEXT_PADDING;
        }
        if (!worth.isEmpty()) {
            gfx.textNoShadow(worth, textX + 2, textY, Theme.GOLD);
        }
    }

    private void refreshTexts() {
        version = status.version();
        if (status.todayKnown()) {
            long net = status.todayNetCents();
            today = I18n.get("hugomarket.status.today", (net >= 0 ? "+" : "−") + Money.compact(Math.abs(net)));
            todayColor = net >= 0 ? Theme.BARGAIN_FRAME : Theme.DANGER;
        }
        if (status.netWorthKnown()) {
            worth = I18n.get("hugomarket.status.worth", Money.compact(status.netWorthCents()));
        }
    }
}
