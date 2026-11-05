package dev.hugomarket.module.dumper;

import dev.hugomarket.HugoMarket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

//? if >=1.21.6 {
/*import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
//?}

final class DumperHud {
    private static final String LABEL_KEY = "hugomarket.dumper.hud";
    private static final int COLOR = 0xFFFF5555;
    private static final int MARGIN = 4;

    private final DumperState state;
    private String label = "";
    private int shownContainers = -1;
    private int shownChatLines = -1;

    DumperHud(DumperState state) {
        this.state = state;
    }

    void register() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(HugoMarket.MOD_ID, "dumper");
        //? if >=1.21.6 {
        /*HudElementRegistry.addLast(id, (graphics, tickCounter) -> draw(graphics));
        *///?} else {
        HudLayerRegistrationCallback.EVENT.register(drawer ->
                drawer.addLayer(IdentifiedLayer.of(id, (graphics, tickCounter) -> draw(graphics))));
        //?}
    }

    private void draw(GuiGraphics graphics) {
        if (!state.isRecording()) {
            return;
        }
        int containers = state.containerCount();
        int chatLines = state.chatLineCount();
        if (containers != shownContainers || chatLines != shownChatLines) {
            shownContainers = containers;
            shownChatLines = chatLines;
            label = I18n.get(LABEL_KEY, containers, chatLines);
        }
        graphics.drawString(Minecraft.getInstance().font, label, MARGIN, MARGIN, COLOR);
    }
}
