package dev.hugomarket.ui;

import dev.hugomarket.config.ModConfig;
import dev.hugomarket.store.HudBoard;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

//? if >=1.21.9 {
/*import net.minecraft.client.input.MouseButtonEvent;
*///?}

public final class HudEditScreen extends Screen {
    private final Screen parent;
    private final HudOverlay overlay;
    private final ModConfig config;
    private final String hint;
    private boolean dragging;
    private int grabX;
    private int grabY;
    private int left;
    private int top;
    private int boxWidth;
    private int boxHeight;

    public HudEditScreen(Screen parent, HudOverlay overlay) {
        super(Component.translatable("hugomarket.hud.move"));
        this.parent = parent;
        this.overlay = overlay;
        this.config = overlay.config().get();
        this.hint = I18n.get("hugomarket.hud.move.hint");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Gfx gfx = new Gfx(graphics);
        List<HudBoard.Line> lines = overlay.editLines();
        boxWidth = overlay.width(gfx, lines);
        boxHeight = HudOverlay.height(lines);
        if (dragging) {
            config.hudX = HudPlacement.fraction(mouseX - grabX, width, boxWidth);
            config.hudY = HudPlacement.fraction(mouseY - grabY, height, boxHeight);
        }
        left = HudPlacement.position(config.hudX, width, boxWidth);
        top = HudPlacement.position(config.hudY, height, boxHeight);
        gfx.textNoShadow(hint, (width - gfx.textWidth(hint)) / 2, height / 2, Theme.TEXT_DIM);
        boolean hovered = mouseX >= left && mouseX < left + boxWidth && mouseY >= top && mouseY < top + boxHeight;
        overlay.paint(gfx, lines, left, top, dragging || hovered ? Theme.ACCENT : Theme.PANEL_BORDER);
    }

    private boolean pressed(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            config.hudX = 0;
            config.hudY = 0;
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || mouseX < left || mouseX >= left + boxWidth || mouseY < top || mouseY >= top + boxHeight) {
            return false;
        }
        dragging = true;
        grabX = (int) mouseX - left;
        grabY = (int) mouseY - top;
        return true;
    }

    //? if >=1.21.9 {
    /*@Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return pressed(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return pressed(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    //?}

    @Override
    public void onClose() {
        overlay.config().save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
