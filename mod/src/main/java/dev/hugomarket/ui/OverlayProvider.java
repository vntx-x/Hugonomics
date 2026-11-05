package dev.hugomarket.ui;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;

public interface OverlayProvider {
    List<OverlayPanel> createPanels(AbstractContainerScreen<?> screen);
}
