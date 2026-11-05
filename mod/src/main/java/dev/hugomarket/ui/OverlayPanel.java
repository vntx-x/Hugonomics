package dev.hugomarket.ui;

import dev.hugomarket.ui.widget.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class OverlayPanel {
    protected final List<Widget> widgets = new ArrayList<>();
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    private boolean visible = true;
    private String title;
    private Supplier<String> headerRight;

    public static int headerHeight() {
        return Theme.HEADER_HEIGHT + Theme.GAP;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    protected void setTitle(String title) {
        this.title = title;
    }

    protected void setHeaderRight(Supplier<String> headerRight) {
        this.headerRight = headerRight;
    }

    protected int contentTop() {
        return y + Theme.PADDING + (title == null ? 0 : headerHeight());
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

    protected void renderFrame(Gfx gfx) {
        gfx.fill(x + 2, y + 2, width, height, Theme.PANEL_SHADOW);
        gfx.fill(x, y, width, height, Theme.PANEL_BG);
        gfx.border(x, y, width, height, Theme.PANEL_BORDER);
        if (title == null) {
            return;
        }
        gfx.fill(x + 1, y + 1, width - 2, Theme.HEADER_HEIGHT, Theme.HEADER_BG);
        gfx.fill(x + 1, y + 1 + Theme.HEADER_HEIGHT, width - 2, 1, Theme.PANEL_BORDER);
        int textY = y + 1 + (Theme.HEADER_HEIGHT - gfx.lineHeight()) / 2 + 1;
        int rightWidth = 0;
        if (headerRight != null) {
            String right = headerRight.get();
            if (!right.isEmpty()) {
                rightWidth = gfx.textWidth(right) + Theme.GAP;
                gfx.textNoShadow(right, x + width - Theme.PADDING - gfx.textWidth(right), textY, Theme.TEXT_DIM);
            }
        }
        gfx.text(gfx.clip(title, width - Theme.PADDING * 2 - rightWidth), x + Theme.PADDING, textY, Theme.ACCENT);
    }

    public void render(Gfx gfx, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        renderFrame(gfx);
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
