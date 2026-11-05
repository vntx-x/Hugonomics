package dev.hugomarket.module.networth;

import dev.hugomarket.model.Money;

import java.util.List;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BalanceParser {
    private static final Pattern LABEL = Pattern.compile("(?i)\\b(geld|money|balance|kontostand|konto|guthaben|coins|bal)\\b");
    private static final Pattern AMOUNT = Pattern.compile("\\$\\s?([0-9][0-9.]*(?:,[0-9]{1,2})?)|([0-9][0-9.]*(?:,[0-9]{1,2})?)\\s?\\$");
    private static final Pattern LABELED_AMOUNT = Pattern.compile(
            "(?i)\\b(?:geld|money|balance|kontostand|konto|guthaben|coins|bal)\\b[^0-9]{0,12}([0-9][0-9.]*(?:,[0-9]+)?)\\s?([kmb])?(?![0-9,.])");

    private BalanceParser() {
    }

    public static OptionalLong fromSidebar(List<String> lines) {
        OptionalLong unlabeled = OptionalLong.empty();
        for (String line : lines) {
            OptionalLong labeled = labeled(line);
            if (labeled.isPresent()) {
                return labeled;
            }
            OptionalLong amount = amount(line);
            if (amount.isPresent() && unlabeled.isEmpty()) {
                unlabeled = amount;
            }
        }
        return unlabeled;
    }

    public static OptionalLong fromMessage(String text) {
        if (!LABEL.matcher(text).find() || text.contains("+") || text.contains("-$")) {
            return OptionalLong.empty();
        }
        OptionalLong amount = amount(text);
        return amount.isPresent() ? amount : labeled(text);
    }

    private static OptionalLong labeled(String line) {
        Matcher m = LABELED_AMOUNT.matcher(line);
        if (!m.find()) {
            return OptionalLong.empty();
        }
        String number = m.group(1).replace(".", "").replace(',', '.');
        double value;
        try {
            value = Double.parseDouble(number);
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
        String suffix = m.group(2) == null ? "" : m.group(2).toLowerCase(java.util.Locale.ROOT);
        double factor = switch (suffix) {
            case "k" -> 1_000;
            case "m" -> 1_000_000;
            case "b" -> 1_000_000_000;
            default -> 1;
        };
        return OptionalLong.of(Math.round(value * factor * 100));
    }

    private static OptionalLong amount(String line) {
        Matcher m = AMOUNT.matcher(line);
        if (!m.find()) {
            return OptionalLong.empty();
        }
        String digits = m.group(1) != null ? m.group(1) : m.group(2);
        return Money.parseCents(digits);
    }
}
