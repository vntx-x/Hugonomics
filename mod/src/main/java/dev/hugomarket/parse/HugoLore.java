package dev.hugomarket.parse;

import dev.hugomarket.model.Money;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HugoLore {
    private static final String MONEY = "(\\$[0-9][0-9.,]*)";
    private static final Pattern PRICE = Pattern.compile("^Preis: " + MONEY + "$");
    private static final Pattern UNIT_PRICE = Pattern.compile("^Preis pro Stück: " + MONEY + "$");
    private static final Pattern SELLER = Pattern.compile("^Verkäufer: (\\S+)$");
    private static final Pattern EXPIRES = Pattern.compile("^Läuft ab in: (.+)$");
    private static final Pattern WORTH = Pattern.compile("^Wert: " + MONEY + "(?: \\(\\+" + MONEY + " durch ([0-9.]+)x\\))?$");
    private static final Pattern DELIVERED = Pattern.compile("^(\\d+)/(\\d+) Geliefert$");
    private static final Pattern PAID = Pattern.compile("^" + MONEY + "/" + MONEY + " Ausgezahlt$");
    private static final Pattern AMOUNT = Pattern.compile("^Anzahl: (\\d+)$");
    private static final Pattern FEE = Pattern.compile("^Erstellgebühr: " + MONEY + "$");
    private static final Pattern SPENT = Pattern.compile("^Insgesamt ausgegeben: " + MONEY + "$");
    private static final Pattern EARNED = Pattern.compile("^Insgesamt verdient: " + MONEY + "$");
    private static final Pattern STATUS = Pattern.compile("^Status: (\\S+)$");

    public record Worth(long valueCents, long bonusCents, double multiplier) {
        public long baseCents() {
            return valueCents - bonusCents;
        }
    }

    public record Progress(long done, long total) {
    }

    public record PaidOut(long paidCents, long totalCents) {
    }

    private HugoLore() {
    }

    public static OptionalLong price(List<String> lore) {
        return money(lore, PRICE);
    }

    public static OptionalLong unitPrice(List<String> lore) {
        return money(lore, UNIT_PRICE);
    }

    public static OptionalLong amount(List<String> lore) {
        return number(lore, AMOUNT);
    }

    public static OptionalLong fee(List<String> lore) {
        return money(lore, FEE);
    }

    public static OptionalLong spentTotal(List<String> lore) {
        return money(lore, SPENT);
    }

    public static OptionalLong earnedTotal(List<String> lore) {
        return money(lore, EARNED);
    }

    public static Optional<String> seller(List<String> lore) {
        Matcher m = find(lore, SELLER);
        return m == null ? Optional.empty() : Optional.of(m.group(1));
    }

    public static Optional<String> status(List<String> lore) {
        Matcher m = find(lore, STATUS);
        return m == null ? Optional.empty() : Optional.of(m.group(1));
    }

    public static OptionalLong expiresInSeconds(List<String> lore) {
        Matcher m = find(lore, EXPIRES);
        return m == null ? OptionalLong.empty() : Durations.parseSeconds(m.group(1));
    }

    public static Optional<Worth> worth(List<String> lore) {
        Matcher m = find(lore, WORTH);
        if (m == null) {
            return Optional.empty();
        }
        OptionalLong value = Money.parseCents(m.group(1));
        if (value.isEmpty()) {
            return Optional.empty();
        }
        long bonus = m.group(2) == null ? 0 : Money.parseCents(m.group(2)).orElse(0);
        double multiplier = m.group(3) == null ? 1.0 : Double.parseDouble(m.group(3));
        return Optional.of(new Worth(value.getAsLong(), bonus, multiplier));
    }

    public static Optional<Progress> delivered(List<String> lore) {
        Matcher m = find(lore, DELIVERED);
        return m == null ? Optional.empty() : Optional.of(new Progress(Long.parseLong(m.group(1)), Long.parseLong(m.group(2))));
    }

    public static Optional<PaidOut> paidOut(List<String> lore) {
        Matcher m = find(lore, PAID);
        if (m == null) {
            return Optional.empty();
        }
        OptionalLong paid = Money.parseCents(m.group(1));
        OptionalLong total = Money.parseCents(m.group(2));
        if (paid.isEmpty() || total.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PaidOut(paid.getAsLong(), total.getAsLong()));
    }

    public static Optional<String> selectedOption(List<String> lore, String header) {
        boolean inSection = header == null;
        for (String line : lore) {
            String trimmed = line.trim();
            if (header != null && trimmed.equals(header)) {
                inSection = true;
                continue;
            }
            if (inSection && trimmed.startsWith("> ")) {
                return Optional.of(trimmed.substring(2).trim());
            }
            if (inSection && header != null && trimmed.endsWith(":")) {
                inSection = false;
            }
        }
        return Optional.empty();
    }

    private static OptionalLong money(List<String> lore, Pattern pattern) {
        Matcher m = find(lore, pattern);
        return m == null ? OptionalLong.empty() : Money.parseCents(m.group(1));
    }

    private static OptionalLong number(List<String> lore, Pattern pattern) {
        Matcher m = find(lore, pattern);
        return m == null ? OptionalLong.empty() : OptionalLong.of(Long.parseLong(m.group(1)));
    }

    private static Matcher find(List<String> lore, Pattern pattern) {
        for (String line : lore) {
            Matcher m = pattern.matcher(line.trim());
            if (m.matches()) {
                return m;
            }
        }
        return null;
    }
}
