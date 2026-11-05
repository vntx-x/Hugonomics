package dev.hugomarket.ui.widget;

import dev.hugomarket.ui.Gfx;

public abstract class Widget {
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public abstract void render(Gfx gfx, int mouseX, int mouseY);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    public boolean keyPressed(int key, int scancode, int modifiers) {
        return false;
    }

    public boolean charTyped(char character) {
        return false;
    }

    public boolean isFocused() {
        return false;
    }

    public void setFocused(boolean focused) {
    }
}
