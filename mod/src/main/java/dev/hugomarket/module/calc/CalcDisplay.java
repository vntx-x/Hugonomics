package dev.hugomarket.module.calc;

import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Widget;
import org.lwjgl.glfw.GLFW;

final class CalcDisplay extends Widget {
    private static final float RESULT_SCALE = 1.5f;
    private static final int ERROR_COLOR = 0xFFFF6B6B;

    private final CalcState state;
    private final Runnable onEscape;
    private boolean focused;

    CalcDisplay(CalcState state, Runnable onEscape) {
        this.state = state;
        this.onEscape = onEscape;
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
        int innerWidth = width - 6;
        String expression = state.expression();
        if (gfx.textWidth(expression) > innerWidth) {
            int start = 0;
            while (start < expression.length() && gfx.textWidth(expression.substring(start)) > innerWidth) {
                start++;
            }
            expression = expression.substring(start);
        }
        gfx.textNoShadow(expression, x + width - 3 - gfx.textWidth(expression), y + 3, Theme.TEXT_DIM);
        CalcEngine.Result result = state.result();
        String text;
        int color;
        if (result.ok()) {
            text = CalcFormat.full(result.value());
            color = Theme.TEXT;
        } else if (state.expression().isEmpty()) {
            text = "0";
            color = Theme.TEXT_DIM;
        } else {
            text = "…";
            color = ERROR_COLOR;
        }
        float scale = gfx.textWidth(text) * RESULT_SCALE > innerWidth ? 1.0f : RESULT_SCALE;
        int textWidth = Math.round(gfx.textWidth(text) * scale);
        gfx.textScaled(text, x + width - 3 - textWidth, y + 14, scale, color);
        if (result.ok() && Math.abs(result.value()) >= 1000) {
            String compact = CalcFormat.compact(result.value());
            gfx.textNoShadow(compact, x + 3, y + 17, Theme.ACCENT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        focused = true;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (!focused) {
            return false;
        }
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> onEscape.run();
            case GLFW.GLFW_KEY_BACKSPACE -> state.backspace();
            case GLFW.GLFW_KEY_DELETE -> state.clear();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_EQUAL -> state.evaluate();
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char character) {
        if (!focused) {
            return false;
        }
        if (character == '=') {
            state.evaluate();
            return true;
        }
        state.input(character);
        return true;
    }
}
