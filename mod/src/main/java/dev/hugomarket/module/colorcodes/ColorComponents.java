package dev.hugomarket.module.colorcodes;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

final class ColorComponents {
    private ColorComponents() {
    }

    static Component render(String input) {
        MutableComponent root = Component.empty();
        for (ColorCodes.Run run : ColorCodes.parse(input)) {
            Style style = Style.EMPTY
                    .withBold(run.bold())
                    .withItalic(run.italic())
                    .withUnderlined(run.underline())
                    .withStrikethrough(run.strikethrough())
                    .withObfuscated(run.obfuscated());
            if (run.color() >= 0) {
                style = style.withColor(TextColor.fromRgb(run.color()));
            }
            root.append(Component.literal(run.text()).withStyle(style));
        }
        return root;
    }
}
