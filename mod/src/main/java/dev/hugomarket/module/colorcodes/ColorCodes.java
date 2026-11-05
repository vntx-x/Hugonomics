package dev.hugomarket.module.colorcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
            char c = input.charAt(i);
            int code = codeLength(input, i);
            if (code == 0) {
                text.append(c);
                i++;
                continue;
            }
            if (!text.isEmpty()) {
                runs.add(new Run(text.toString(), color, bold, italic, underline, strike, obfuscated));
                text.setLength(0);
            }
            if (code == 8) {
                color = Integer.parseInt(input, i + 2, i + 8, 16);
                bold = italic = underline = strike = obfuscated = false;
            } else {
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
            }
            i += code;
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
            int code = codeLength(input, i);
            if (code == 0) {
                out.append(input.charAt(i));
                i++;
            } else {
                i += code;
            }
        }
        return out.toString();
    }

    public static int visibleLength(String input) {
        return strip(input).length();
    }

    public static String gradient(String input, int from, int to) {
        String plain = strip(input);
        int letters = 0;
        for (int i = 0; i < plain.length(); i++) {
            if (plain.charAt(i) != ' ') {
                letters++;
            }
        }
        StringBuilder out = new StringBuilder(plain.length() * 9);
        int index = 0;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c == ' ') {
                out.append(c);
                continue;
            }
            float t = letters <= 1 ? 0f : (float) index / (letters - 1);
            out.append(PREFIX).append('#').append(hex(mix(from, to, t))).append(c);
            index++;
        }
        return out.toString();
    }

    public static String hex(int rgb) {
        return String.format(Locale.ROOT, "%06X", rgb & 0xFFFFFF);
    }

    public static int parseHex(String text) {
        String clean = text.startsWith("#") ? text.substring(1) : text;
        if (clean.length() != 6) {
            return -1;
        }
        try {
            return Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int mix(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = Math.round(((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = Math.round((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static int codeLength(String input, int at) {
        if (input.charAt(at) != PREFIX || at + 1 >= input.length()) {
            return 0;
        }
        char next = input.charAt(at + 1);
        if (next == '#') {
            if (at + 8 > input.length()) {
                return 0;
            }
            for (int i = at + 2; i < at + 8; i++) {
                if (Character.digit(input.charAt(i), 16) < 0) {
                    return 0;
                }
            }
            return 8;
        }
        char key = Character.toLowerCase(next);
        return LEGACY_KEYS.indexOf(key) >= 0 || FORMAT_KEYS.indexOf(key) >= 0 ? 2 : 0;
    }
}
