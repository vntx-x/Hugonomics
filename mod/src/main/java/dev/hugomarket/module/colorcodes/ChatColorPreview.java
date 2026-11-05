package dev.hugomarket.module.colorcodes;

import dev.hugomarket.mixin.ChatScreenAccessor;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.Theme;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class ChatColorPreview {
    private static final int BOX_HEIGHT = 14;
    private static final int BOTTOM_OFFSET = 16;
    private static final String[] COMMANDS = {"/rename ", "/sign "};

    private String lastInput = "";
    private Component preview;
    private String counter = "";

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
            update(text);
        }
        if (preview == null) {
            return;
        }
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int counterWidth = gfx.textWidth(counter);
        int width = gfx.componentWidth(preview) + counterWidth + 14;
        int x = 2;
        int y = height - BOTTOM_OFFSET - BOX_HEIGHT - 2;
        gfx.fill(x, y, width, BOX_HEIGHT, Theme.PANEL_BG);
        gfx.border(x, y, width, BOX_HEIGHT, Theme.PANEL_BORDER);
        gfx.component(preview, x + 4, y + 3);
        gfx.textNoShadow(counter, x + width - counterWidth - 4, y + 3, Theme.TEXT_DIM);
    }

    private void update(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String command : COMMANDS) {
            if (lower.startsWith(command) && text.length() > command.length()) {
                String body = text.substring(command.length());
                preview = ColorComponents.render(body);
                counter = Integer.toString(ColorCodes.visibleLength(body));
                return;
            }
        }
        preview = null;
    }
}
