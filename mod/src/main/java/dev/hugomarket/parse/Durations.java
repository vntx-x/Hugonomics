package dev.hugomarket.parse;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Durations {
    private static final Pattern PART = Pattern.compile("(\\d+)\\s*([dhms])");

    private Durations() {
    }

    public static OptionalLong parseSeconds(String text) {
        Matcher m = PART.matcher(text);
        long seconds = 0;
        boolean any = false;
        while (m.find()) {
            any = true;
            long value = Long.parseLong(m.group(1));
            seconds += switch (m.group(2).charAt(0)) {
                case 'd' -> value * 86400;
                case 'h' -> value * 3600;
                case 'm' -> value * 60;
                default -> value;
            };
        }
        return any ? OptionalLong.of(seconds) : OptionalLong.empty();
    }
}
