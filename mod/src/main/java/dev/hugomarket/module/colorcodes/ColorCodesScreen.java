package dev.hugomarket.module.colorcodes;

import dev.hugomarket.ui.Gfx;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

//? if >=1.21.9 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

final class ColorCodesScreen extends Screen {
    private final Screen parent;
    private final ColorCodesPanel panel;
    private boolean closing;

    ColorCodesScreen(Screen parent, ColorCodesState state) {
        super(Component.translatable("hugomarket.colors.title"));
        this.parent = parent;
        panel = new ColorCodesPanel(state, this::closeToParent);
    }

    @Override
    protected void init() {
        int panelHeight = ColorCodesPanel.preferredHeight();
        panel.setBounds((width - ColorCodesPanel.WIDTH) / 2, (height - panelHeight) / 2, ColorCodesPanel.WIDTH, panelHeight);
        panel.focus();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        panel.render(new Gfx(graphics), mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    private void closeToParent() {
        if (closing) {
            return;
        }
        closing = true;
        minecraft.setScreen(parent);
    }

    private boolean handleKey(int key, int scancode, int modifiers) {
        if (panel.keyPressed(key, scancode, modifiers)) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            closeToParent();
            return true;
        }
        return false;
    }

    //? if >=1.21.9 {
    /*@Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return panel.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return handleKey(event.key(), event.scancode(), event.modifiers()) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return (Character.isBmpCodePoint(event.codepoint()) && panel.charTyped((char) event.codepoint())) || super.charTyped(event);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return handleKey(key, scancode, modifiers) || super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return panel.charTyped(character) || super.charTyped(character, modifiers);
    }
    //?}
}
