package dev.hugomarket.config;

import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
    private static final int WIDTH = 420;
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

    private boolean handleKey(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_LEFT) {
            panel.step(key == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
            return true;
        }
        return false;
    }

    //? if >=1.21.9 {
    /*@Override
    public boolean keyPressed(KeyEvent event) {
        return handleKey(event.key()) || super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return panel.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }
    *///?} else {
    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return handleKey(key) || super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }
    //?}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return panel.mouseScrolled(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private static final class HelpPanel extends OverlayPanel {
        private static final int LINE_HEIGHT = 11;
        private static final int HEADER_HEIGHT = 14;

        private final List<Chip> tabs = new ArrayList<>();
        private final List<String> tabLabels = new ArrayList<>();
        private final List<List<String>> sections = new ArrayList<>();
        private int selected;
        private List<String> wrapped = List.of();
        private int wrappedWidth = -1;
        private int wrappedSection = -1;
        private int scroll;
        private int contentTop;

        HelpPanel() {
            for (int i = 0; i < SECTIONS.length; i++) {
                int index = i;
                String section = SECTIONS[i];
                String label = I18n.get("hugomarket.help." + section + ".tab");
                tabLabels.add(label);
                tabs.add(add(new Chip(label, () -> selected == index, () -> select(index))));
                List<String> lines = new ArrayList<>();
                for (int n = 1; n <= 8; n++) {
                    String key = "hugomarket.help." + section + "." + n;
                    if (!I18n.exists(key)) {
                        break;
                    }
                    lines.add(I18n.get(key));
                }
                sections.add(lines);
            }
        }

        void step(int delta) {
            select(Math.floorMod(selected + delta, SECTIONS.length));
        }

        private void select(int index) {
            selected = index;
            scroll = 0;
        }

        @Override
        protected void layout() {
            int innerX = x + Theme.PADDING;
            int innerWidth = width - Theme.PADDING * 2;
            int cursorX = innerX;
            int cursorY = y + Theme.PADDING + HEADER_HEIGHT;
            int chipHeight = Theme.CHIP_HEIGHT + 2;
            Font font = Minecraft.getInstance().font;
            for (int i = 0; i < tabs.size(); i++) {
                Chip tab = tabs.get(i);
                int chipWidth = font.width(tabLabels.get(i)) + 14;
                if (cursorX + chipWidth > innerX + innerWidth) {
                    cursorX = innerX;
                    cursorY += chipHeight + Theme.GAP;
                }
                tab.setBounds(cursorX, cursorY, chipWidth, chipHeight);
                cursorX += chipWidth + Theme.GAP;
            }
            contentTop = cursorY + chipHeight + Theme.GAP * 2;
            wrappedWidth = -1;
        }

        private void wrap(Gfx gfx) {
            int innerWidth = width - Theme.PADDING * 2 - 8;
            if (wrappedWidth == innerWidth && wrappedSection == selected) {
                return;
            }
            wrappedWidth = innerWidth;
            wrappedSection = selected;
            List<String> out = new ArrayList<>();
            for (String line : sections.get(selected)) {
                StringBuilder current = new StringBuilder("• ");
                for (String word : line.split(" ")) {
                    String candidate = current.length() <= 2 ? current + word : current + " " + word;
                    if (gfx.textWidth(candidate) > innerWidth && current.length() > 2) {
                        out.add(current.toString());
                        current = new StringBuilder("   " + word);
                    } else {
                        current = new StringBuilder(candidate);
                    }
                }
                out.add(current.toString());
                out.add("");
            }
            wrapped = out;
        }

        @Override
        public void render(Gfx gfx, int mouseX, int mouseY) {
            super.render(gfx, mouseX, mouseY);
            gfx.text(I18n.get("hugomarket.help.title"), x + Theme.PADDING, y + Theme.PADDING, Theme.ACCENT);
            wrap(gfx);
            int innerX = x + Theme.PADDING;
            int innerWidth = width - Theme.PADDING * 2;
            int innerHeight = y + height - Theme.PADDING - contentTop;
            gfx.fill(innerX, contentTop - Theme.GAP, innerWidth, innerHeight + Theme.GAP, Theme.FIELD_BG);
            gfx.textNoShadow(I18n.get("hugomarket.help." + SECTIONS[selected] + ".title"), innerX + 4, contentTop + 1, Theme.ACCENT);
            int listTop = contentTop + LINE_HEIGHT + Theme.GAP;
            int listHeight = innerHeight - LINE_HEIGHT - Theme.GAP;
            int maxScroll = Math.max(0, wrapped.size() * LINE_HEIGHT - listHeight);
            scroll = Math.max(0, Math.min(scroll, maxScroll));
            gfx.scissor(innerX, listTop, innerWidth, listHeight);
            int lineY = listTop - scroll;
            for (String line : wrapped) {
                if (lineY + LINE_HEIGHT >= listTop && lineY <= listTop + listHeight) {
                    gfx.textNoShadow(line, innerX + 4, lineY, Theme.TEXT);
                }
                lineY += LINE_HEIGHT;
            }
            gfx.unscissor();
            if (maxScroll > 0) {
                int barHeight = Math.max(8, listHeight * listHeight / (wrapped.size() * LINE_HEIGHT));
                int barY = listTop + (listHeight - barHeight) * scroll / maxScroll;
                gfx.fill(innerX + innerWidth - 3, barY, 2, barHeight, Theme.TEXT_DIM);
            }
            String hint = I18n.get("hugomarket.help.nav");
            gfx.textNoShadow(hint, x + width - Theme.PADDING - gfx.textWidth(hint), y + Theme.PADDING + 1, Theme.TEXT_DIM);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            scroll -= (int) (delta * 22);
            return true;
        }
    }
}
