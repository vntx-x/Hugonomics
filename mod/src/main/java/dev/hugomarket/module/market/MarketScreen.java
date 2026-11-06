package dev.hugomarket.module.market;

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

final class MarketScreen extends Screen {
    private static final int MARGIN = 20;
    private static final int MAX_WIDTH = 420;

    private final Screen parent;
    private final MarketPanel panel;
    private static final int REFRESH_TICKS = 100;

    private final MarketData data;
    private int ticks;

    MarketScreen(Screen parent, MarketPanel panel, MarketData data) {
        super(Component.translatable("hugomarket.market.title"));
        this.parent = parent;
        this.panel = panel;
        this.data = data;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(MAX_WIDTH, width - MARGIN * 2);
        panel.setBounds((width - panelWidth) / 2, MARGIN, panelWidth, height - MARGIN * 2);
        panel.focusSearch();
        data.refreshItems(System.currentTimeMillis());
    }

    @Override
    public void tick() {
        if (++ticks % REFRESH_TICKS == 0) {
            panel.tick();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        panel.render(new Gfx(graphics), mouseX, mouseY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean handleKey(int key, int scancode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return panel.keyPressed(key, scancode, modifiers);
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return panel.mouseScrolled(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
}
