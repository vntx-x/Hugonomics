package dev.hugomarket.module.calc;

import dev.hugomarket.mixin.AnvilScreenAccessor;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import dev.hugomarket.ui.widget.ListView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;

public final class CalcPanel extends OverlayPanel {
    public static final int WIDTH = 150;
    private static final int DISPLAY_HEIGHT = 34;
    private static final int KEY_HEIGHT = 14;
    private static final int COLUMNS = 5;
    private static final String[][] KEYS = {
            {"7", "8", "9", "÷", "⌫"},
            {"4", "5", "6", "×", "C"},
            {"1", "2", "3", "−", "("},
            {"0", ",", "%", "+", ")"},
            {"K", "M", "B", "=", ""},
    };

    private final CalcState state;
    private final Runnable onClose;
    private final CalcDisplay display;
    private final List<Chip> keys = new ArrayList<>();
    private final Chip copy;
    private final Chip toPrice;
    private final ListView<CalcState.HistoryEntry> history;

    public CalcPanel(CalcState state, Runnable onClose) {
        this.state = state;
        this.onClose = onClose;
        setTitle(I18n.get("hugomarket.calc.panel_title"));
        setHeaderRight(() -> I18n.get("hugomarket.calc.key_hint"));
        display = add(new CalcDisplay(state, onClose));
        for (String[] row : KEYS) {
            for (String label : row) {
                keys.add(label.isEmpty() ? null : add(new Chip(label, () -> false, () -> press(label))));
            }
        }
        copy = add(new Chip(I18n.get("hugomarket.calc.copy"), () -> false, this::copyResult));
        toPrice = add(new Chip(I18n.get("hugomarket.calc.to_price"), CalcPanel::anvilOpen, this::applyToPrice));
        history = add(new ListView<>(10, this::renderHistory, (entry, button) -> state.recall(entry)));
        state.onChange(() -> history.setRows(List.copyOf(state.history())));
        setVisible(state.isOpen());
    }

    public static int preferredHeight() {
        return Theme.PADDING * 2 + headerHeight() + DISPLAY_HEIGHT + Theme.GAP + KEYS.length * (KEY_HEIGHT + Theme.GAP) + Theme.CHIP_HEIGHT + Theme.GAP + 10 * 4;
    }

    public void focus() {
        display.setFocused(true);
    }

    public void unfocus() {
        display.setFocused(false);
    }

    @Override
    protected void layout() {
        int innerX = x + Theme.PADDING;
        int innerWidth = width - Theme.PADDING * 2;
        int cursorY = contentTop();
        display.setBounds(innerX, cursorY, innerWidth, DISPLAY_HEIGHT);
        cursorY += DISPLAY_HEIGHT + Theme.GAP;
        int keyWidth = (innerWidth - Theme.GAP * (COLUMNS - 1)) / COLUMNS;
        for (int i = 0; i < keys.size(); i++) {
            Chip key = keys.get(i);
            if (key != null) {
                key.setBounds(innerX + (i % COLUMNS) * (keyWidth + Theme.GAP), cursorY + (i / COLUMNS) * (KEY_HEIGHT + Theme.GAP), keyWidth, KEY_HEIGHT);
            }
        }
        cursorY += KEYS.length * (KEY_HEIGHT + Theme.GAP);
        int half = (innerWidth - Theme.GAP) / 2;
        copy.setBounds(innerX, cursorY, half, Theme.CHIP_HEIGHT);
        toPrice.setBounds(innerX + half + Theme.GAP, cursorY, half, Theme.CHIP_HEIGHT);
        cursorY += Theme.CHIP_HEIGHT + Theme.GAP;
        history.setBounds(innerX, cursorY, innerWidth, Math.max(0, y + height - Theme.PADDING - cursorY));
    }

    private void press(String label) {
        display.setFocused(true);
        switch (label) {
            case "⌫" -> state.backspace();
            case "C" -> state.clear();
            case "=" -> state.evaluate();
            default -> state.input(label.charAt(0));
        }
    }

    private void copyResult() {
        if (state.result().ok()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(CalcFormat.asInput(state.result().value()));
        }
    }

    private void applyToPrice() {
        if (!state.result().ok() || !(Minecraft.getInstance().screen instanceof AnvilScreen anvil)) {
            return;
        }
        ((AnvilScreenAccessor) anvil).hugomarket$nameField().setValue(Long.toString(CalcFormat.wholeDollars(state.result().value())));
    }

    private static boolean anvilOpen() {
        return Minecraft.getInstance().screen instanceof AnvilScreen;
    }

    private void renderHistory(Gfx gfx, CalcState.HistoryEntry entry, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
        String result = CalcFormat.full(entry.value());
        int resultWidth = gfx.textWidth(result);
        gfx.textNoShadow(gfx.clip(entry.expression(), rowWidth - resultWidth - 6), rowX + 1, rowY + 1, Theme.TEXT_DIM);
        gfx.textNoShadow(result, rowX + rowWidth - resultWidth - 3, rowY + 1, Theme.TEXT);
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
