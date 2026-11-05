package dev.hugomarket.ui;

import dev.hugomarket.ui.widget.Widget;

import java.util.ArrayList;
import java.util.List;

public abstract class OverlayPanel {
    protected final List<Widget> widgets = new ArrayList<>();
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    private boolean visible = true;

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    protected <W extends Widget> W add(W widget) {
        widgets.add(widget);
        return widget;
    }

    public void setBounds(int x, int y, int width, int height) {
        boolean changed = this.x != x || this.y != y || this.width != width || this.height != height;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        if (changed) {
            layout();
        }
    }

    protected abstract void layout();

    public boolean contains(double mouseX, double mouseY) {
        return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void render(Gfx gfx, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        gfx.fill(x, y, width, height, Theme.PANEL_BG);
        gfx.border(x, y, width, height, Theme.PANEL_BORDER);
        for (Widget widget : widgets) {
            widget.render(gfx, mouseX, mouseY);
        }
    }

    public void renderBehind(Gfx gfx, int mouseX, int mouseY) {
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = contains(mouseX, mouseY);
        for (Widget widget : widgets) {
            if (widget.isFocused() && !widget.contains(mouseX, mouseY)) {
                widget.setFocused(false);
            }
        }
        if (!inside) {
            return false;
        }
        for (Widget widget : widgets) {
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        for (Widget widget : widgets) {
            if (widget.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return true;
    }

    public boolean keyPressed(int key, int scancode, int modifiers) {
        for (Widget widget : widgets) {
            if (widget.isFocused()) {
                return widget.keyPressed(key, scancode, modifiers);
            }
        }
        return false;
    }

    public boolean charTyped(char character) {
        for (Widget widget : widgets) {
            if (widget.isFocused()) {
                return widget.charTyped(character);
            }
        }
        return false;
    }

    public boolean hasFocus() {
        if (!visible) {
            return false;
        }
        for (Widget widget : widgets) {
            if (widget.isFocused()) {
                return true;
            }
        }
        return false;
    }

    public void onClosed() {
    }
}
