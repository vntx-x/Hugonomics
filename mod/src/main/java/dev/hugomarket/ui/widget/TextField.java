package dev.hugomarket.ui.widget;

import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public final class TextField extends Widget {
    private static final int MAX_LENGTH = 64;
    private static final long BLINK_MS = 500;

    private final String placeholder;
    private final Consumer<String> onChange;
    private final StringBuilder value = new StringBuilder();
    private int caret;
    private boolean focused;

    public TextField(String placeholder, String initial, Consumer<String> onChange) {
        this.placeholder = placeholder;
        this.onChange = onChange;
        value.append(initial);
        caret = value.length();
    }

    public String value() {
        return value.toString();
    }

    public void setValue(String text) {
        value.setLength(0);
        value.append(text, 0, Math.min(text.length(), MAX_LENGTH));
        caret = value.length();
        onChange.accept(value());
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        gfx.fill(x, y, width, height, Theme.FIELD_BG);
        gfx.border(x, y, width, height, focused ? Theme.FIELD_BORDER_FOCUS : Theme.FIELD_BORDER);
        int textX = x + 3;
        int textY = y + (height - gfx.lineHeight()) / 2 + 1;
        int innerWidth = width - 6;
        if (value.isEmpty() && !focused) {
            gfx.textNoShadow(gfx.clip(placeholder, innerWidth), textX, textY, Theme.TEXT_PLACEHOLDER);
            return;
        }
        String text = value.toString();
        int caretOffset = gfx.textWidth(text.substring(0, caret));
        int scroll = Math.max(0, caretOffset - innerWidth + 2);
        gfx.scissor(x + 2, y, width - 4, height);
        gfx.textNoShadow(text, textX - scroll, textY, Theme.TEXT);
        if (focused && (System.currentTimeMillis() / BLINK_MS) % 2 == 0) {
            gfx.fill(textX - scroll + caretOffset, textY - 1, 1, gfx.lineHeight() + 1, Theme.ACCENT);
        }
        gfx.unscissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        focused = true;
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            clear();
        } else {
            caret = value.length();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (!focused) {
            return false;
        }
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> focused = false;
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (ctrl) {
                    deleteWordBefore();
                } else if (caret > 0) {
                    value.deleteCharAt(--caret);
                    changed();
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (caret < value.length()) {
                    value.deleteCharAt(caret);
                    changed();
                }
            }
            case GLFW.GLFW_KEY_LEFT -> caret = Math.max(0, caret - 1);
            case GLFW.GLFW_KEY_RIGHT -> caret = Math.min(value.length(), caret + 1);
            case GLFW.GLFW_KEY_HOME -> caret = 0;
            case GLFW.GLFW_KEY_END -> caret = value.length();
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) {
                    insert(Minecraft.getInstance().keyboardHandler.getClipboard());
                }
            }
            case GLFW.GLFW_KEY_A -> {
                if (ctrl) {
                    clear();
                }
            }
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char character) {
        if (!focused || character < ' ') {
            return false;
        }
        insert(String.valueOf(character));
        return true;
    }

    public void clear() {
        value.setLength(0);
        caret = 0;
        changed();
    }

    private void insert(String text) {
        int room = MAX_LENGTH - value.length();
        if (room <= 0) {
            return;
        }
        String clean = text.replace("\n", "").replace("\r", "");
        if (clean.length() > room) {
            clean = clean.substring(0, room);
        }
        value.insert(caret, clean);
        caret += clean.length();
        changed();
    }

    private void deleteWordBefore() {
        int end = caret;
        while (caret > 0 && value.charAt(caret - 1) == ' ') {
            caret--;
        }
        while (caret > 0 && value.charAt(caret - 1) != ' ') {
            caret--;
        }
        value.delete(caret, end);
        changed();
    }

    private void changed() {
        onChange.accept(value());
    }
}
