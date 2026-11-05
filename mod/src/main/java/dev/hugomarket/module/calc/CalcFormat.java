package dev.hugomarket.module.calc;

import dev.hugomarket.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CalcFormat {
    private static final double MAX_EXACT = 1e15;

    private CalcFormat() {
    }

    public static String full(double value) {
        if (Math.abs(value) >= MAX_EXACT) {
            return String.format(java.util.Locale.ROOT, "%.3e", value);
        }
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        long whole = rounded.longValue();
        String grouped = Money.format(Math.abs(whole) * 100).substring(1);
        String sign = rounded.signum() < 0 ? "-" : "";
        if (rounded.scale() <= 0) {
            return sign + grouped;
        }
        String fraction = rounded.abs().remainder(BigDecimal.ONE).movePointRight(rounded.scale()).toPlainString();
        return sign + grouped + "," + fraction;
    }

    public static String compact(double value) {
        if (Math.abs(value) >= MAX_EXACT) {
            return full(value);
        }
        return Money.compact(Math.round(value * 100));
    }

    public static String asInput(double value) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return rounded.toPlainString().replace('.', ',');
    }

    public static long wholeDollars(double value) {
        return Math.round(value);
    }
}
