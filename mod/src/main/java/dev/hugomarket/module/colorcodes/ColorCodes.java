package dev.hugomarket.module.colorcodes;

import java.util.ArrayList;
import java.util.List;

public final class ColorCodes {
    public static final char PREFIX = '&';
    public static final int[] LEGACY = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
    };
    private static final String LEGACY_KEYS = "0123456789abcdef";
    private static final String FORMAT_KEYS = "klmnor";

    public record Run(String text, int color, boolean bold, boolean italic, boolean underline, boolean strikethrough, boolean obfuscated) {
    }

    private ColorCodes() {
    }

    public static List<Run> parse(String input) {
        List<Run> runs = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int color = -1;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strike = false;
        boolean obfuscated = false;
        int i = 0;
        while (i < input.length()) {
            if (!isCode(input, i)) {
                text.append(input.charAt(i));
                i++;
                continue;
            }
            if (!text.isEmpty()) {
                runs.add(new Run(text.toString(), color, bold, italic, underline, strike, obfuscated));
                text.setLength(0);
            }
            char key = Character.toLowerCase(input.charAt(i + 1));
            int legacy = LEGACY_KEYS.indexOf(key);
            if (legacy >= 0) {
                color = LEGACY[legacy];
                bold = italic = underline = strike = obfuscated = false;
            } else {
                switch (key) {
                    case 'k' -> obfuscated = true;
                    case 'l' -> bold = true;
                    case 'm' -> strike = true;
                    case 'n' -> underline = true;
                    case 'o' -> italic = true;
                    default -> {
                        color = -1;
                        bold = italic = underline = strike = obfuscated = false;
                    }
                }
            }
            i += 2;
        }
        if (!text.isEmpty()) {
            runs.add(new Run(text.toString(), color, bold, italic, underline, strike, obfuscated));
        }
        return runs;
    }

    public static String strip(String input) {
        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            if (isCode(input, i)) {
                i += 2;
            } else {
                out.append(input.charAt(i));
                i++;
            }
        }
        return out.toString();
    }

    public static int visibleLength(String input) {
        return strip(input).length();
    }

    private static boolean isCode(String input, int at) {
        if (input.charAt(at) != PREFIX || at + 1 >= input.length()) {
            return false;
        }
        char key = Character.toLowerCase(input.charAt(at + 1));
        return LEGACY_KEYS.indexOf(key) >= 0 || FORMAT_KEYS.indexOf(key) >= 0;
    }
}
