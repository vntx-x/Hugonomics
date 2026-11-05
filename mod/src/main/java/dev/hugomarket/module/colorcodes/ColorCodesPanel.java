package dev.hugomarket.module.colorcodes;

import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.Label;
import dev.hugomarket.ui.widget.Swatch;
import dev.hugomarket.ui.widget.TextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class ColorCodesPanel extends OverlayPanel {
    static final int WIDTH = 270;
    private static final int PREVIEW_HEIGHT = 18;
    private static final int SWATCH_HEIGHT = 12;
    private static final int TEXT_MAX = 400;
    private static final String[][] FORMATS = {
            {"hugomarket.colors.bold", "&l"}, {"hugomarket.colors.italic", "&o"}, {"hugomarket.colors.underline", "&n"},
            {"hugomarket.colors.strike", "&m"}, {"hugomarket.colors.magic", "&k"}, {"hugomarket.colors.reset", "&r"},
    };

    private final ColorCodesState state;
    private final Runnable onClose;
    private final Chip rename;
    private final Chip sign;
    private final Label counter;
    private final TextField text;
    private final List<Swatch> swatches = new ArrayList<>();
    private final List<Chip> formats = new ArrayList<>();
    private final TextField hex;
    private final Chip insertHex;
    private final TextField gradientFrom;
    private final TextField gradientTo;
    private final Chip applyGradient;
    private final Chip toChat;
    private final Chip copy;
    private final Chip clear;
    private Component preview = Component.empty();

    ColorCodesPanel(ColorCodesState state, Runnable onClose) {
        this.state = state;
        this.onClose = onClose;
        rename = add(new Chip("/rename", () -> state.target() == ColorCodesState.Target.RENAME, () -> state.setTarget(ColorCodesState.Target.RENAME)));
        sign = add(new Chip("/sign", () -> state.target() == ColorCodesState.Target.SIGN, () -> state.setTarget(ColorCodesState.Target.SIGN)));
        counter = add(new Label(this::counterText, Theme.TEXT_DIM));
        text = add(new TextField(I18n.get("hugomarket.colors.placeholder"), state.text(), TEXT_MAX, this::onTextChanged));
        for (int i = 0; i < ColorCodes.LEGACY.length; i++) {
            String code = "&" + Character.forDigit(i, 16);
            swatches.add(add(new Swatch(ColorCodes.LEGACY[i], () -> text.insertAtCaret(code))));
        }
        for (String[] format : FORMATS) {
            formats.add(add(new Chip(I18n.get(format[0]), () -> false, () -> text.insertAtCaret(format[1]))));
        }
        hex = add(new TextField("#RRGGBB", "", 7, value -> { }));
        insertHex = add(new Chip(I18n.get("hugomarket.colors.insert_hex"), () -> false, this::insertHex));
        gradientFrom = add(new TextField("#55FF55", "", 7, value -> { }));
        gradientTo = add(new TextField("#C77DFF", "", 7, value -> { }));
        applyGradient = add(new Chip(I18n.get("hugomarket.colors.gradient"), () -> false, this::applyGradient));
        toChat = add(new Chip(I18n.get("hugomarket.colors.to_chat"), () -> false, this::toChat));
        copy = add(new Chip(I18n.get("hugomarket.colors.copy"), () -> false, this::copy));
        clear = add(new Chip(I18n.get("hugomarket.colors.clear"), () -> false, text::clear));
        preview = ColorComponents.render(state.text());
    }

    static int preferredHeight() {
        return Theme.PADDING * 2 + Theme.CHIP_HEIGHT + Theme.GAP + Theme.FIELD_HEIGHT + Theme.GAP + PREVIEW_HEIGHT + Theme.GAP
                + SWATCH_HEIGHT + Theme.GAP + Theme.CHIP_HEIGHT + Theme.GAP + Theme.FIELD_HEIGHT + Theme.GAP + Theme.FIELD_HEIGHT
                + Theme.GAP + Theme.CHIP_HEIGHT;
    }

    void focus() {
        text.setFocused(true);
    }

    @Override
    protected void layout() {
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        int cursorY = y + Theme.PADDING;
        rename.setBounds(innerX, cursorY, 44, Theme.CHIP_HEIGHT);
        sign.setBounds(innerX + 44 + Theme.GAP, cursorY, 36, Theme.CHIP_HEIGHT);
        counter.setBounds(innerX + 86, cursorY, innerWidth - 86, Theme.CHIP_HEIGHT);
        cursorY += Theme.CHIP_HEIGHT + Theme.GAP;
        text.setBounds(innerX, cursorY, innerWidth, Theme.FIELD_HEIGHT);
        cursorY += Theme.FIELD_HEIGHT + Theme.GAP + PREVIEW_HEIGHT + Theme.GAP;
        int swatchWidth = (innerWidth - Theme.GAP * (swatches.size() - 1)) / swatches.size();
        for (int i = 0; i < swatches.size(); i++) {
            swatches.get(i).setBounds(innerX + i * (swatchWidth + Theme.GAP), cursorY, swatchWidth, SWATCH_HEIGHT);
        }
        cursorY += SWATCH_HEIGHT + Theme.GAP;
        int formatWidth = (innerWidth - Theme.GAP * (formats.size() - 1)) / formats.size();
        for (int i = 0; i < formats.size(); i++) {
            formats.get(i).setBounds(innerX + i * (formatWidth + Theme.GAP), cursorY, formatWidth, Theme.CHIP_HEIGHT);
        }
        cursorY += Theme.CHIP_HEIGHT + Theme.GAP;
        int fieldWidth = 58;
        hex.setBounds(innerX, cursorY, fieldWidth, Theme.FIELD_HEIGHT);
        insertHex.setBounds(innerX + fieldWidth + Theme.GAP, cursorY, 70, Theme.FIELD_HEIGHT);
        cursorY += Theme.FIELD_HEIGHT + Theme.GAP;
        gradientFrom.setBounds(innerX, cursorY, fieldWidth, Theme.FIELD_HEIGHT);
        gradientTo.setBounds(innerX + fieldWidth + Theme.GAP, cursorY, fieldWidth, Theme.FIELD_HEIGHT);
        applyGradient.setBounds(innerX + (fieldWidth + Theme.GAP) * 2, cursorY, 70, Theme.FIELD_HEIGHT);
        cursorY += Theme.FIELD_HEIGHT + Theme.GAP;
        int third = (innerWidth - Theme.GAP * 2) / 3;
        toChat.setBounds(innerX, cursorY, third, Theme.CHIP_HEIGHT);
        copy.setBounds(innerX + third + Theme.GAP, cursorY, third, Theme.CHIP_HEIGHT);
        clear.setBounds(innerX + (third + Theme.GAP) * 2, cursorY, third, Theme.CHIP_HEIGHT);
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        super.render(gfx, mouseX, mouseY);
        int previewY = text.y() + text.height() + Theme.GAP;
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        gfx.fill(innerX, previewY, innerWidth, PREVIEW_HEIGHT, Theme.FIELD_BG);
        gfx.scissor(innerX, previewY, innerWidth, PREVIEW_HEIGHT);
        gfx.component(preview, innerX + 4, previewY + (PREVIEW_HEIGHT - gfx.lineHeight()) / 2 + 1);
        gfx.unscissor();
    }

    private void onTextChanged(String value) {
        state.setText(value);
        preview = ColorComponents.render(value);
    }

    private String counterText() {
        int visible = state.visibleLength();
        String base = I18n.get("hugomarket.colors.counter", visible, state.target().limit, state.command().length());
        return state.overLimit() ? "! " + base : base;
    }

    private void insertHex() {
        int rgb = ColorCodes.parseHex(hex.value().trim());
        if (rgb >= 0) {
            text.insertAtCaret("&#" + ColorCodes.hex(rgb));
        }
    }

    private void applyGradient() {
        int from = ColorCodes.parseHex(gradientFrom.value().trim());
        int to = ColorCodes.parseHex(gradientTo.value().trim());
        if (from < 0) {
            from = 0x55FF55;
        }
        if (to < 0) {
            to = 0xC77DFF;
        }
        text.setValue(ColorCodes.gradient(text.value(), from, to));
    }

    private void toChat() {
        if (state.text().isEmpty()) {
            return;
        }
        onClose.run();
        //? if >=1.21.9 {
        /*Minecraft.getInstance().setScreen(new ChatScreen(state.command(), false));
        *///?} else
        Minecraft.getInstance().setScreen(new ChatScreen(state.command()));
    }

    private void copy() {
        Minecraft.getInstance().keyboardHandler.setClipboard(state.command());
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        boolean handled = super.keyPressed(key, scancode, modifiers);
        if (handled && key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose.run();
        }
        return handled;
    }
}
