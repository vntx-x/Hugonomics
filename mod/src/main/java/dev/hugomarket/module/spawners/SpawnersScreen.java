package dev.hugomarket.module.spawners;

import dev.hugomarket.store.MarketSession;
import dev.hugomarket.ui.Gfx;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

//? if >=1.21.9 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

final class SpawnersScreen extends Screen {
    private static final int WIDTH = 620;
    private static final int MAX_HEIGHT = 290;
    private static final int MARGIN = 12;

    private final Screen parent;
    private final ExchangePanel panel;
    private int ticks;

    SpawnersScreen(Screen parent, MarketSession market, SpawnerExchange exchange, Map<String, Integer> owned) {
        super(Component.translatable("hugomarket.spawners.title"));
        this.parent = parent;
        this.panel = new ExchangePanel(market, exchange, owned);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(WIDTH, width - MARGIN * 2);
        int panelHeight = Math.min(MAX_HEIGHT, height - MARGIN * 2);
        panel.setBounds((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    @Override
    public void tick() {
        panel.tick(++ticks);
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
        if (panel.keyPressed(key, scancode, modifiers)) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return panel.mouseScrolled(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
}
