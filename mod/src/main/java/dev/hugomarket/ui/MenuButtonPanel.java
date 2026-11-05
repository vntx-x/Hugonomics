package dev.hugomarket.ui;

import dev.hugomarket.mixin.AbstractContainerScreenAccessor;
import dev.hugomarket.ui.widget.Chip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;

public final class MenuButtonPanel extends OverlayPanel {
    private static final int HEIGHT = 13;
    private static final int GAP_ABOVE = 2;

    private final Chip button;

    public MenuButtonPanel(AbstractContainerScreen<?> screen, Runnable open) {
        button = add(new Chip(I18n.get("hugomarket.menu.button"), () -> false, open));
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int width = 72;
        int left = accessor.hugomarket$leftPos();
        int top = Math.max(GAP_ABOVE, accessor.hugomarket$topPos() - HEIGHT - GAP_ABOVE);
        setBounds(left, top, width, HEIGHT);
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
    }
}
