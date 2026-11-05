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

    public record Payout(long totalCents, long baseCents, long bonusCents, double multiplier) {
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
}
