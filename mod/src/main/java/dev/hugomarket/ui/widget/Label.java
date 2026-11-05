package dev.hugomarket.ui.widget;

import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;

import java.util.function.Supplier;

public final class Label extends Widget {
    private final Supplier<String> text;
    private final int color;

    public Label(Supplier<String> text, int color) {
        this.text = text;
        this.color = color;
    }

    public Label(Supplier<String> text) {
        this(text, Theme.TEXT_DIM);
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        gfx.textNoShadow(gfx.clip(text.get(), width), x, y + (height - gfx.lineHeight()) / 2 + 1, color);
    }
}
