package dev.hugomarket.module.calc;

import dev.hugomarket.mixin.ChatScreenAccessor;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

final class ChatInlineCalc {
    private static final int BOX_HEIGHT = 12;
    private static final int BOTTOM_OFFSET = 16;

    private String lastInput = "";
    private String display = "";

    void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof ChatScreen chat) {
                ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) -> render(chat, new Gfx(graphics)));
            }
        });
    }

    private void render(ChatScreen chat, Gfx gfx) {
        EditBox input = ((ChatScreenAccessor) chat).hugomarket$input();
        if (input == null) {
            return;
        }
        String text = input.getValue();
        if (!text.equals(lastInput)) {
            lastInput = text;
            display = compute(text);
        }
        if (display.isEmpty()) {
            return;
        }
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int width = gfx.textWidth(display) + 8;
        int x = 2;
        int y = height - BOTTOM_OFFSET - BOX_HEIGHT - 2;
        gfx.fill(x, y, width, BOX_HEIGHT, Theme.PANEL_BG);
        gfx.border(x, y, width, BOX_HEIGHT, Theme.PANEL_BORDER);
        gfx.textNoShadow(display, x + 4, y + 2, Theme.TEXT);
    }

    private static String compute(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("=") || trimmed.length() < 2) {
            return "";
        }
        CalcEngine.Result result = CalcEngine.evaluate(trimmed.substring(1));
        if (!result.ok()) {
            return "";
        }
        String full = CalcFormat.full(result.value());
        String compact = CalcFormat.compact(result.value());
        return compact.equals(full) ? "= " + full : "= " + full + "  (" + compact + ")";
    }

    static boolean isChat(Screen screen) {
        return screen instanceof ChatScreen;
    }
}
