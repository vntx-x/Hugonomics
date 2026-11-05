package dev.hugomarket.module.networth;

import dev.hugomarket.model.Money;

import java.util.List;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BalanceParser {
    private static final Pattern LABEL = Pattern.compile("(?i)\\b(geld|money|balance|kontostand|konto|guthaben|coins|bal)\\b");
    private static final Pattern AMOUNT = Pattern.compile("\\$\\s?([0-9][0-9.]*(?:,[0-9]{1,2})?)|([0-9][0-9.]*(?:,[0-9]{1,2})?)\\s?\\$");

    private BalanceParser() {
    }

    public static OptionalLong fromSidebar(List<String> lines) {
        OptionalLong unlabeled = OptionalLong.empty();
        for (String line : lines) {
            OptionalLong amount = amount(line);
            if (amount.isEmpty()) {
                continue;
            }
            if (LABEL.matcher(line).find()) {
                return amount;
            }
            if (unlabeled.isEmpty()) {
                unlabeled = amount;
            }
        }
        return unlabeled;
    }

    public static OptionalLong fromMessage(String text) {
        if (!LABEL.matcher(text).find() || text.contains("+") || text.contains("-$")) {
            return OptionalLong.empty();
        }
        return amount(text);
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
