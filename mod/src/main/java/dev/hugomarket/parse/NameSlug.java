package dev.hugomarket.parse;

import java.text.Normalizer;
import java.util.Locale;

public final class NameSlug {
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
    private static final String ASCII = "abcdefghijklmnopqrstuvwxyz";
    private static final int MAX_LENGTH = 48;

    private NameSlug() {
    }

    public static String of(String name) {
        StringBuilder out = new StringBuilder(name.length());
        boolean lastUnderscore = true;
        for (char c : Normalizer.normalize(fromSmallCaps(name), Normalizer.Form.NFKD).toLowerCase(Locale.ROOT).toCharArray()) {
            boolean keep = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (keep) {
                out.append(c);
                lastUnderscore = false;
            } else if (!lastUnderscore) {
                out.append('_');
                lastUnderscore = true;
            }
            if (out.length() >= MAX_LENGTH) {
                break;
            }
        }
        int end = out.length();
        while (end > 0 && out.charAt(end - 1) == '_') {
            end--;
        }
        return out.substring(0, end);
    }

    static String fromSmallCaps(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int index = SMALL_CAPS.indexOf(c);
            out.append(index >= 0 ? ASCII.charAt(index) : c);
        }
        return out.toString();
    }
}
