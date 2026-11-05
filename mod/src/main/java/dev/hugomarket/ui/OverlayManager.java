package dev.hugomarket.ui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class OverlayManager {
    private static volatile OverlayManager instance;

    private final List<OverlayProvider> providers = new CopyOnWriteArrayList<>();
    private ScreenSession session;

    public OverlayManager() {
        instance = this;
        ScreenEvents.AFTER_INIT.register(this::afterInit);
    }

    public void addProvider(OverlayProvider provider) {
        providers.add(provider);
    }

    public void removeProvider(OverlayProvider provider) {
        providers.remove(provider);
    }

    public static boolean charTyped(int codePoint, int modifiers) {
        OverlayManager manager = instance;
        if (manager == null || manager.session == null || !Character.isBmpCodePoint(codePoint)) {
            return false;
        }
        return manager.session.charTyped((char) codePoint);
    }

    private void afterInit(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            session = null;
            return;
        }
        List<OverlayPanel> panels = new ArrayList<>();
        for (OverlayProvider provider : providers) {
            panels.addAll(provider.createPanels(containerScreen));
        }
        if (panels.isEmpty()) {
            session = null;
            return;
        }
        ScreenSession created = new ScreenSession(containerScreen, panels);
        session = created;
        ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> created.render(graphics, mouseX, mouseY));
        ScreenEvents.remove(screen).register(s -> created.close());
        ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, horizontal, vertical) -> !created.mouseScrolled(mouseX, mouseY, vertical));
        //? if >=1.21.9 {
        /*ScreenMouseEvents.allowMouseClick(screen).register((s, event) -> !created.mouseClicked(event.x(), event.y(), event.button()));
        ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !created.keyPressed(event.key(), event.scancode(), event.modifiers()));
        *///?} else {
        ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) -> !created.mouseClicked(mouseX, mouseY, button));
        ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) -> !created.keyPressed(key, scancode, modifiers));
        //?}
    }

    private final class ScreenSession {
        private final AbstractContainerScreen<?> screen;
        private final List<OverlayPanel> panels;

        ScreenSession(AbstractContainerScreen<?> screen, List<OverlayPanel> panels) {
            this.screen = screen;
            this.panels = panels;
        }

        void render(GuiGraphics graphics, int mouseX, int mouseY) {
            Gfx gfx = new Gfx(graphics);
            for (OverlayPanel panel : panels) {
                panel.renderBehind(gfx, mouseX, mouseY);
            }
            for (OverlayPanel panel : panels) {
                panel.render(gfx, mouseX, mouseY);
            }
        }

        boolean mouseClicked(double mouseX, double mouseY, int button) {
            boolean consumed = false;
            for (OverlayPanel panel : panels) {
                consumed |= panel.mouseClicked(mouseX, mouseY, button);
            }
            return consumed;
        }

        boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            for (OverlayPanel panel : panels) {
                if (panel.mouseScrolled(mouseX, mouseY, delta)) {
                    return true;
                }
            }
            return false;
        }

        boolean keyPressed(int key, int scancode, int modifiers) {
            for (OverlayPanel panel : panels) {
                if (panel.hasFocus()) {
                    panel.keyPressed(key, scancode, modifiers);
                    return key != GLFW.GLFW_KEY_ESCAPE || true;
                }
            }
            return false;
        }

        boolean charTyped(char character) {
            if (Minecraft.getInstance().screen != screen) {
                return false;
            }
            for (OverlayPanel panel : panels) {
                if (panel.hasFocus()) {
                    return panel.charTyped(character);
                }
            }
            return false;
        }

        void close() {
            for (OverlayPanel panel : panels) {
                panel.onClosed();
            }
            if (session == this) {
                session = null;
            }
        }
    }
}
