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

    public static void rightOf(AbstractContainerScreen<?> screen, OverlayPanel panel, int preferredWidth, int height) {
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int left = accessor.hugomarket$leftPos() + accessor.hugomarket$imageWidth() + GAP;
        int width = Math.min(preferredWidth, screenWidth - left - GAP);
        if (width < 80) {
            left = GAP;
            width = Math.max(80, accessor.hugomarket$leftPos() - GAP * 2);
        }
        panel.setBounds(left, accessor.hugomarket$topPos(), width, height);
    }
}
