package dev.hugomarket.container;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.ScreenKind;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public record ScreenUpdate(ScreenKind kind, ContainerView view, ContainerSnapshot snapshot, AbstractContainerScreen<?> screen) {
}
