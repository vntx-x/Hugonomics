package dev.hugomarket.ui;

import dev.hugomarket.HugoMarket;
import dev.hugomarket.core.ServerDetector;
import dev.hugomarket.store.HudBoard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

//? if >=1.21.6 {
/*import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
//?}

public final class HudOverlay {
    private static final int MARGIN = 4;

    private final HudBoard board;
    private final ServerDetector server;
    private List<HudBoard.Line> measuredFor;
    private int boxWidth;

    public HudOverlay(HudBoard board, ServerDetector server) {
        this.board = board;
        this.server = server;
    }

    public void register() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(HugoMarket.MOD_ID, "hud");
        //? if >=1.21.6 {
        /*HudElementRegistry.addLast(id, (graphics, tickCounter) -> draw(graphics));
        *///?} else {
        HudLayerRegistrationCallback.EVENT.register(drawer ->
                drawer.addLayer(IdentifiedLayer.of(id, (graphics, tickCounter) -> draw(graphics))));
        //?}
    }

    private void draw(GuiGraphics graphics) {
        List<HudBoard.Line> lines = board.lines();
        Minecraft client = Minecraft.getInstance();
        if (lines.isEmpty() || client.options.hideGui || client.screen != null || !server.active()) {
            return;
        }
        Gfx gfx = new Gfx(graphics);
        if (lines != measuredFor) {
            measuredFor = lines;
            boxWidth = 0;
            for (HudBoard.Line line : lines) {
                boxWidth = Math.max(boxWidth, gfx.textWidth(line.text()));
            }
            boxWidth += Theme.PADDING * 2;
        }
        int boxHeight = lines.size() * Theme.LINE + Theme.PADDING * 2 - 2;
        gfx.fill(MARGIN, MARGIN, boxWidth, boxHeight, Theme.PANEL_BG);
        gfx.border(MARGIN, MARGIN, boxWidth, boxHeight, Theme.PANEL_BORDER);
        int textY = MARGIN + Theme.PADDING;
        for (HudBoard.Line line : lines) {
            gfx.textNoShadow(line.text(), MARGIN + Theme.PADDING, textY, line.color());
            textY += Theme.LINE;
        }
    }
}
