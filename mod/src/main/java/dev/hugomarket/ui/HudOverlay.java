package dev.hugomarket.ui;

import dev.hugomarket.HugoMarket;
import dev.hugomarket.config.ConfigStore;
import dev.hugomarket.core.ServerDetector;
import dev.hugomarket.store.HudBoard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

//? if >=1.21.6 {
/*import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
//?}

public final class HudOverlay {
    private final HudBoard board;
    private final ServerDetector server;
    private final ConfigStore config;
    private List<HudBoard.Line> measuredFor;
    private int boxWidth;
    private List<HudBoard.Line> preview;

    public HudOverlay(HudBoard board, ServerDetector server, ConfigStore config) {
        this.board = board;
        this.server = server;
        this.config = config;
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

    public ConfigStore config() {
        return config;
    }

    List<HudBoard.Line> editLines() {
        if (!board.lines().isEmpty()) {
            return board.lines();
        }
        if (preview == null) {
            preview = List.of(new HudBoard.Line(I18n.get("hugomarket.hud.preview"), Theme.TEXT));
        }
        return preview;
    }

    int width(Gfx gfx, List<HudBoard.Line> lines) {
        if (lines != measuredFor) {
            measuredFor = lines;
            boxWidth = 0;
            for (HudBoard.Line line : lines) {
                boxWidth = Math.max(boxWidth, gfx.textWidth(line.text()));
            }
            boxWidth += Theme.PADDING * 2;
        }
        return boxWidth;
    }

    static int height(List<HudBoard.Line> lines) {
        return lines.size() * Theme.LINE + Theme.PADDING * 2 - 2;
    }

    void paint(Gfx gfx, List<HudBoard.Line> lines, int left, int top, int border) {
        int width = width(gfx, lines);
        int height = height(lines);
        gfx.fill(left, top, width, height, Theme.PANEL_BG);
        gfx.border(left, top, width, height, border);
        int textY = top + Theme.PADDING;
        for (HudBoard.Line line : lines) {
            gfx.textNoShadow(line.text(), left + Theme.PADDING, textY, line.color());
            textY += Theme.LINE;
        }
    }

    private void draw(GuiGraphics graphics) {
        List<HudBoard.Line> lines = board.lines();
        Minecraft client = Minecraft.getInstance();
        if (lines.isEmpty() || client.options.hideGui || client.screen != null || !server.active()) {
            return;
        }
        Gfx gfx = new Gfx(graphics);
        int left = HudPlacement.position(config.get().hudX, graphics.guiWidth(), width(gfx, lines));
        int top = HudPlacement.position(config.get().hudY, graphics.guiHeight(), height(lines));
        paint(gfx, lines, left, top, Theme.PANEL_BORDER);
    }
}
