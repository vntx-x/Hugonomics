package dev.hugomarket.parse;

import dev.hugomarket.model.Money;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatPatterns {
    private static final String PREFIX = "^\\[HugoSMP\\] ";
    private static final String MONEY = "(\\$[0-9][0-9.,]*)";
    private static final Pattern PAYOUT = Pattern.compile(PREFIX + "\\+" + MONEY
            + "(?: \\(Basis: " + MONEY + ", Bonus: \\+" + MONEY + " durch ([0-9.]+)x\\))?$");
    private static final Pattern AUCTION_BOUGHT = Pattern.compile(PREFIX + "Du hast die Auktion erfolgreich gekauft!$");
    private static final Pattern ORDER_CREATED = Pattern.compile(PREFIX + "Order erstellt\\.$");
    private static final Pattern SOLD_WORD = Pattern.compile("(?i)\\bverkauft\\b");
    private static final Pattern ANY_MONEY = Pattern.compile("\\$\\s?([0-9][0-9.]*(?:,[0-9]{1,2})?)");
    private static final Pattern SOLD_STACK = Pattern.compile("(?i)(\\d+)x\\s+([^$]+?)\\s+(?:für|for)\\s+\\$");
    private static final Pattern SOLD_SINGLE = Pattern.compile("(?i)(?:du hast|deine auktion|auktion|dein|deine)\\s+([^$]+?)\\s+(?:für|for)\\s+\\$");

    public record Payout(long totalCents, long baseCents, long bonusCents, double multiplier) {
    }

    public record Sale(long priceCents, int count, String item) {
    }

    private ChatPatterns() {
    }

    public static String normalize(String text) {
        return text.replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    public static Optional<Payout> payout(String text) {
        Matcher m = PAYOUT.matcher(normalize(text));
        if (!m.matches()) {
            return Optional.empty();
        }
        long total = Money.parseCents(m.group(1)).orElse(-1);
        if (total < 0) {
            return Optional.empty();
        }
        if (m.group(2) == null) {
            return Optional.of(new Payout(total, total, 0, 1.0));
        }
        long base = Money.parseCents(m.group(2)).orElse(total);
        long bonus = Money.parseCents(m.group(3)).orElse(0);
        return Optional.of(new Payout(total, base, bonus, Double.parseDouble(m.group(4))));
    }

    public static boolean isAuctionBought(String text) {
        return AUCTION_BOUGHT.matcher(normalize(text)).matches();
    }

    public static boolean isOrderCreated(String text) {
        return ORDER_CREATED.matcher(normalize(text)).matches();
    }

    public static Optional<Sale> auctionSold(String text) {
        String line = normalize(text);
        if (!SOLD_WORD.matcher(line).find() || line.contains("gekauft")) {
            return Optional.empty();
        }
        Matcher money = ANY_MONEY.matcher(line);
        if (!money.find()) {
            return Optional.empty();
        }
        long cents = Money.parseCents(money.group(1)).orElse(-1);
        if (cents <= 0) {
            return Optional.empty();
        }
        Matcher stack = SOLD_STACK.matcher(line);
        if (stack.find()) {
            return Optional.of(new Sale(cents, Integer.parseInt(stack.group(1)), stack.group(2).trim()));
        }
        Matcher single = SOLD_SINGLE.matcher(line);
        if (single.find()) {
            return Optional.of(new Sale(cents, 1, single.group(1).trim()));
        }
        return Optional.of(new Sale(cents, 0, ""));
    }
}
