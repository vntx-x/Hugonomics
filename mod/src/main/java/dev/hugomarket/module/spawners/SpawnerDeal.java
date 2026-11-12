package dev.hugomarket.module.spawners;

import dev.hugomarket.model.Money;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record SpawnerDeal(int count, long unitCents, long referenceUnitCents) {
    private static final Pattern PRICE = Pattern.compile("^([0-9][0-9.,]*)(k|m|mio|b|mrd)?$");
    private static final long MAX_DOLLARS = 1_000_000_000_000L;

    long totalCents() {
        return unitCents * count;
    }

    long referenceTotalCents() {
        return referenceUnitCents * count;
    }

    OptionalDouble ratio() {
        return referenceUnitCents <= 0 ? OptionalDouble.empty() : OptionalDouble.of((double) unitCents / referenceUnitCents);
    }

    static OptionalLong parseCount(String text) {
        String digits = text.trim();
        if (digits.isEmpty() || digits.length() > 6 || !digits.chars().allMatch(Character::isDigit)) {
            return OptionalLong.empty();
        }
        long value = Long.parseLong(digits);
        return value <= 0 ? OptionalLong.empty() : OptionalLong.of(value);
    }

    static OptionalLong parsePriceCents(String text) {
        Matcher m = PRICE.matcher(text.trim().toLowerCase(Locale.ROOT).replace("$", "").replace(" ", ""));
        if (!m.matches()) {
            return OptionalLong.empty();
        }
        String number = m.group(1);
        String suffix = m.group(2);
        if (suffix == null) {
            OptionalLong cents = Money.parseCents(number);
            return cents.isPresent() && cents.getAsLong() > 0 && cents.getAsLong() <= MAX_DOLLARS * 100 ? cents : OptionalLong.empty();
        }
        BigDecimal dollars;
        try {
            dollars = new BigDecimal(number.replace(',', '.')).multiply(BigDecimal.TEN.pow(multiplierExponent(suffix)));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
        if (dollars.signum() <= 0 || dollars.compareTo(BigDecimal.valueOf(MAX_DOLLARS)) > 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(dollars.movePointRight(2).longValue());
    }

    private static int multiplierExponent(String suffix) {
        return switch (suffix) {
            case "k" -> 3;
            case "m", "mio" -> 6;
            default -> 9;
        };
    }
}
