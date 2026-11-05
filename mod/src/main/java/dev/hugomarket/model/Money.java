package dev.hugomarket.model;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Money {
    private static final Pattern AMOUNT = Pattern.compile("^([+-])?\\s*\\$?\\s*([0-9][0-9.]*)(?:,([0-9]{1,2}))?$");
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T"};

    private Money() {
    }

    public static OptionalLong parseCents(String text) {
        Matcher m = AMOUNT.matcher(text.trim());
        if (!m.matches()) {
            return OptionalLong.empty();
        }
        String digits = m.group(2).replace(".", "");
        if (digits.isEmpty()) {
            return OptionalLong.empty();
        }
        long cents;
        try {
            cents = Long.parseLong(digits) * 100;
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
        String fraction = m.group(3);
        if (fraction != null) {
            cents += fraction.length() == 1 ? Long.parseLong(fraction) * 10 : Long.parseLong(fraction);
        }
        return OptionalLong.of("-".equals(m.group(1)) ? -cents : cents);
    }

    public static String format(long cents) {
        long abs = Math.abs(cents);
        StringBuilder out = new StringBuilder();
        if (cents < 0) {
            out.append('-');
        }
        out.append('$').append(group(abs / 100));
        long rest = abs % 100;
        if (rest != 0) {
            out.append(',').append(rest < 10 ? "0" : "").append(rest);
        }
        return out.toString();
    }

    public static String compact(long cents) {
        double dollars = Math.abs(cents) / 100.0;
        String sign = cents < 0 ? "-" : "";
        if (dollars < 1000) {
            return sign + format(Math.abs(cents)).substring(1);
        }
        int tier = 0;
        while (dollars >= 1000 && tier < SUFFIXES.length - 1) {
            dollars /= 1000;
            tier++;
        }
        String number = dollars >= 100 ? String.valueOf(Math.round(dollars)) : trimZero(String.format(java.util.Locale.ROOT, "%.1f", dollars));
        return sign + number + SUFFIXES[tier];
    }

    private static String trimZero(String value) {
        return value.endsWith(".0") ? value.substring(0, value.length() - 2) : value;
    }

    private static String group(long value) {
        String digits = Long.toString(value);
        StringBuilder out = new StringBuilder(digits.length() + digits.length() / 3);
        int lead = digits.length() % 3;
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (i - lead) % 3 == 0) {
                out.append('.');
            }
            out.append(digits.charAt(i));
        }
        return out.toString();
    }
}
