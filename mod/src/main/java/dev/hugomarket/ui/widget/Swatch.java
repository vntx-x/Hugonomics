package dev.hugomarket.ui.widget;

import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;

public final class Swatch extends Widget {
    private final int rgb;
    private final Runnable onClick;

    public Swatch(int rgb, Runnable onClick) {
        this.rgb = rgb;
        this.onClick = onClick;
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        gfx.fill(x, y, width, height, 0xFF000000 | rgb);
        gfx.border(x, y, width, height, contains(mouseX, mouseY) ? Theme.ACCENT : Theme.FIELD_BORDER);
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
