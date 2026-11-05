package dev.hugomarket.ui.widget;

import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class Chip extends Widget {
    private final Supplier<String> label;
    private final BooleanSupplier active;
    private final Runnable onClick;

    public Chip(String label, BooleanSupplier active, Runnable onClick) {
        this(() -> label, active, onClick);
    }

    public Chip(Supplier<String> label, BooleanSupplier active, Runnable onClick) {
        this.label = label;
        this.active = active;
        this.onClick = onClick;
    }

    public int preferredWidth(Gfx gfx) {
        return gfx.textWidth(label.get()) + 10;
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        boolean on = active.getAsBoolean();
        boolean hovered = contains(mouseX, mouseY);
        gfx.fill(x, y, width, height, on ? Theme.CHIP_BG_ACTIVE : hovered ? Theme.CHIP_BG_HOVER : Theme.CHIP_BG);
        if (on) {
            gfx.border(x, y, width, height, Theme.CHIP_BORDER_ACTIVE);
        } else if (hovered) {
            gfx.border(x, y, width, height, Theme.FIELD_BORDER);
        }
        String text = gfx.clip(label.get(), width - 6);
        gfx.textNoShadow(text, x + (width - gfx.textWidth(text)) / 2, y + (height - gfx.lineHeight()) / 2 + 1, on || hovered ? Theme.TEXT : Theme.TEXT_DIM);
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
