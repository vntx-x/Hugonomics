package dev.hugomarket.ui.widget;

import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;

import java.util.function.BooleanSupplier;

public final class Chip extends Widget {
    private final String label;
    private final BooleanSupplier active;
    private final Runnable onClick;

    public Chip(String label, BooleanSupplier active, Runnable onClick) {
        this.label = label;
        this.active = active;
        this.onClick = onClick;
    }

    public int preferredWidth(Gfx gfx) {
        return gfx.textWidth(label) + 8;
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        boolean hovered = contains(mouseX, mouseY);
        int bg = active.getAsBoolean() ? Theme.CHIP_BG_ACTIVE : hovered ? Theme.CHIP_BG_HOVER : Theme.CHIP_BG;
        gfx.fill(x, y, width, height, bg);
        gfx.textNoShadow(label, x + 4, y + (height - gfx.lineHeight()) / 2 + 1, active.getAsBoolean() ? Theme.TEXT : Theme.TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        onClick.run();
        return true;
    }
}
