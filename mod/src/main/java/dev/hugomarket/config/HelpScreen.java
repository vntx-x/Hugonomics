package dev.hugomarket.config;

import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

public final class HelpScreen extends Screen {
    private static final int WIDTH = 400;
    private static final int MARGIN = 16;
    private static final String[] SECTIONS = {"ah", "sell", "orders", "chests", "shop", "market", "calc", "menu", "rules"};

    private final Screen parent;
    private final HelpPanel panel = new HelpPanel();

    public HelpScreen(Screen parent) {
        super(Component.translatable("hugomarket.help.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(WIDTH, width - MARGIN * 2);
        panel.setBounds((width - panelWidth) / 2, MARGIN, panelWidth, height - MARGIN * 2);
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

    //? if >=1.21.9 {
    /*@Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }
    *///?} else {
    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scancode, modifiers);
    }
    //?}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return panel.mouseScrolled(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private static final class HelpPanel extends OverlayPanel {
        private record Line(String text, int color, int indent) {
        }

        private final List<Line> source = new ArrayList<>();
        private List<Line> wrapped = List.of();
        private int wrappedWidth = -1;
        private int scroll;

        HelpPanel() {
            source.add(new Line(I18n.get("hugomarket.help.title"), Theme.ACCENT, 0));
            source.add(new Line(I18n.get("hugomarket.help.intro"), Theme.TEXT_DIM, 0));
            source.add(new Line("", Theme.TEXT, 0));
            for (String section : SECTIONS) {
                source.add(new Line(I18n.get("hugomarket.help." + section + ".title"), Theme.ACCENT, 0));
                for (int i = 1; i <= 8; i++) {
                    String key = "hugomarket.help." + section + "." + i;
                    if (!I18n.exists(key)) {
                        break;
                    }
                    source.add(new Line("• " + I18n.get(key), Theme.TEXT, 6));
                }
                source.add(new Line("", Theme.TEXT, 0));
            }
        }

        @Override
        protected void layout() {
            wrappedWidth = -1;
        }

        private void wrap(Gfx gfx) {
            int innerWidth = width - Theme.PADDING * 2;
            if (wrappedWidth == innerWidth) {
                return;
            }
            wrappedWidth = innerWidth;
            List<Line> out = new ArrayList<>();
            for (Line line : source) {
                int available = innerWidth - line.indent();
                if (line.text().isEmpty() || gfx.textWidth(line.text()) <= available) {
                    out.add(line);
                    continue;
                }
                StringBuilder current = new StringBuilder();
                for (String word : line.text().split(" ")) {
                    String candidate = current.isEmpty() ? word : current + " " + word;
                    if (gfx.textWidth(candidate) > available && !current.isEmpty()) {
                        out.add(new Line(current.toString(), line.color(), line.indent()));
                        current = new StringBuilder("  " + word);
                    } else {
                        current = new StringBuilder(candidate);
                    }
                }
                if (!current.isEmpty()) {
                    out.add(new Line(current.toString(), line.color(), line.indent()));
                }
            }
            wrapped = out;
        }

        @Override
        public void render(Gfx gfx, int mouseX, int mouseY) {
            super.render(gfx, mouseX, mouseY);
            wrap(gfx);
            int lineHeight = 11;
            int innerHeight = height - Theme.PADDING * 2;
            int maxScroll = Math.max(0, wrapped.size() * lineHeight - innerHeight);
            scroll = Math.max(0, Math.min(scroll, maxScroll));
            gfx.scissor(x + Theme.PADDING, y + Theme.PADDING, width - Theme.PADDING * 2, innerHeight);
            int lineY = y + Theme.PADDING - scroll;
            for (Line line : wrapped) {
                if (lineY + lineHeight >= y && lineY <= y + height) {
                    gfx.textNoShadow(line.text(), x + Theme.PADDING + line.indent(), lineY, line.color());
                }
                lineY += lineHeight;
            }
            gfx.unscissor();
            if (maxScroll > 0) {
                int barHeight = Math.max(8, innerHeight * innerHeight / (wrapped.size() * lineHeight));
                int barY = y + Theme.PADDING + (innerHeight - barHeight) * scroll / maxScroll;
                gfx.fill(x + width - 3, barY, 2, barHeight, Theme.TEXT_DIM);
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            scroll -= (int) (delta * 22);
            return true;
        }
    }
}
