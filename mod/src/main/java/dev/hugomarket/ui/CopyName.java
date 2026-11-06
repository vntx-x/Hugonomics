package dev.hugomarket.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class CopyName {
    private CopyName() {
    }

    public static void copy(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.keyboardHandler.setClipboard(name);
        minecraft.gui.setOverlayMessage(Component.translatable("hugomarket.copied", name), false);
    }
}
