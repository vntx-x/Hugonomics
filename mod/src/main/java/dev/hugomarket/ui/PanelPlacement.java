package dev.hugomarket.ui;

import dev.hugomarket.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class PanelPlacement {
    private static final int GAP = 4;

    private PanelPlacement() {
    }

    public static void rightOf(AbstractContainerScreen<?> screen, OverlayPanel panel, int preferredWidth) {
        rightOf(screen, panel, preferredWidth, ((AbstractContainerScreenAccessor) screen).hugomarket$imageHeight());
    }

    public static void leftOf(AbstractContainerScreen<?> screen, OverlayPanel panel, int preferredWidth, int height) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int available = accessor.hugomarket$leftPos() - GAP * 2;
        if (available < 80) {
            rightOf(screen, panel, preferredWidth, height);
            return;
        }
        int width = Math.min(preferredWidth, available);
        panel.setBounds(accessor.hugomarket$leftPos() - GAP - width, accessor.hugomarket$topPos(), width, height);
    }

    public static void sidebarRight(AbstractContainerScreen<?> screen, OverlayPanel panel, int minWidth) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        var window = Minecraft.getInstance().getWindow();
        int left = accessor.hugomarket$leftPos() + accessor.hugomarket$imageWidth() + GAP;
        int width = window.getGuiScaledWidth() - left - GAP;
        if (width < minWidth) {
            rightOf(screen, panel, minWidth);
            return;
        }
        panel.setBounds(left, GAP, width, window.getGuiScaledHeight() - GAP * 2);
    }

    public static void rightOf(AbstractContainerScreen<?> screen, OverlayPanel panel, int preferredWidth, int height) {
        rightOf(screen, panel, preferredWidth, height, 0);
    }

    public static void rightOf(AbstractContainerScreen<?> screen, OverlayPanel panel, int preferredWidth, int height, int offsetY) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int left = accessor.hugomarket$leftPos() + accessor.hugomarket$imageWidth() + GAP;
        int width = Math.min(preferredWidth, screenWidth - left - GAP);
        if (width < 80) {
            left = GAP;
            width = Math.max(80, accessor.hugomarket$leftPos() - GAP * 2);
        }
        panel.setBounds(left, accessor.hugomarket$topPos() + offsetY, width, height);
    }
}
