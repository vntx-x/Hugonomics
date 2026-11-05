package dev.hugomarket.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public final class Gfx {
    private final GuiGraphics graphics;
    private final Font font;

    public Gfx(GuiGraphics graphics) {
        this.graphics = graphics;
        this.font = Minecraft.getInstance().font;
    }

    public int lineHeight() {
        return font.lineHeight;
    }

    public int textWidth(String text) {
        return font.width(text);
    }

    public void fill(int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + height, argb);
    }

    public void border(int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + 1, argb);
        graphics.fill(x, y + height - 1, x + width, y + height, argb);
        graphics.fill(x, y, x + 1, y + height, argb);
        graphics.fill(x + width - 1, y, x + width, y + height, argb);
    }

    public void text(String text, int x, int y, int argb) {
        graphics.drawString(font, text, x, y, argb);
    }

    public void textNoShadow(String text, int x, int y, int argb) {
        graphics.drawString(font, text, x, y, argb, false);
    }

    public void textScaled(String text, int x, int y, float scale, int argb) {
        //? if >=1.21.6 {
        /*graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, text, 0, 0, argb, false);
        graphics.pose().popMatrix();
        *///?} else {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, argb, false);
        graphics.pose().popPose();
        //?}
    }

    public String clip(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int budget = maxWidth - font.width(ellipsis);
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end)) > budget) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    public void item(ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x, y);
    }

    public void scissor(int x, int y, int width, int height) {
        graphics.enableScissor(x, y, x + width, y + height);
    }

    public void unscissor() {
        graphics.disableScissor();
    }

    public void itemTooltip(ItemStack stack, int mouseX, int mouseY) {
        //? if >=1.21.6 {
        /*graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
        *///?} else
        graphics.renderTooltip(font, stack, mouseX, mouseY);
    }

    public void tooltip(List<Component> lines, int mouseX, int mouseY) {
        //? if >=1.21.6 {
        /*graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
        *///?} else
        graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }
}
