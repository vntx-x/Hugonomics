package dev.hugomarket.module.dumper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DumpFileNames {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_TITLE = 40;

    private DumpFileNames() {
    }

    public static String container(LocalDateTime time, String title, int containerSlots) {
        return STAMP.format(time) + "_" + sanitize(title) + "_" + containerSlots + ".json";
    }

    public static String chat(LocalDateTime time) {
        return "chat-" + DAY.format(time) + ".jsonl";
    }

    static String sanitize(String title) {
        StringBuilder out = new StringBuilder(title.length());
        boolean lastUnderscore = true;
        for (int i = 0; i < title.length() && out.length() < MAX_TITLE; i++) {
            char c = title.charAt(i);
            boolean keep = c < 0x2E80 && (Character.isLetterOrDigit(c) || c == '-');
            if (keep) {
                out.append(c);
                lastUnderscore = false;
            } else if (!lastUnderscore) {
                out.append('_');
                lastUnderscore = true;
            }
        }
        int end = out.length();
        while (end > 0 && out.charAt(end - 1) == '_') {
            end--;
        }
        return end == 0 ? "gui" : out.substring(0, end);
    }
}
