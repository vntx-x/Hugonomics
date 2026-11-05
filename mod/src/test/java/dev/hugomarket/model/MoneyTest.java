package dev.hugomarket.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {
    @Test
    void parsesGermanGroupingAndDecimals() {
        assertEquals(172_000_000_000L, Money.parseCents("$1.720.000.000").getAsLong());
        assertEquals(4224, Money.parseCents("$42,24").getAsLong());
        assertEquals(1436, Money.parseCents("+$14,36").getAsLong());
        assertEquals(66, Money.parseCents("$0,66").getAsLong());
        assertEquals(400, Money.parseCents("$4").getAsLong());
        assertEquals(150, Money.parseCents("$1,5").getAsLong());
        assertEquals(-250, Money.parseCents("-$2,50").getAsLong());
    }

    @Test
    void rejectsGarbage() {
        assertTrue(Money.parseCents("Preis").isEmpty());
        assertTrue(Money.parseCents("$").isEmpty());
        assertTrue(Money.parseCents("1,234.56").isEmpty());
    }

    @Test
    void formatsLikeHugo() {
        assertEquals("$42,24", Money.format(4224));
        assertEquals("$1.720.000.000", Money.format(172_000_000_000L));
        assertEquals("$0,05", Money.format(5));
        assertEquals("-$2,50", Money.format(-250));
    }

    @Test
    void compactsToMagnitudes() {
        assertEquals("950", Money.compact(95_000));
        assertEquals("1.4K", Money.compact(140_000));
        assertEquals("12M", Money.compact(1_200_000_000L));
        assertEquals("3.1B", Money.compact(310_000_000_000L));
        assertEquals("172M", Money.compact(17_200_000_000L));
        assertEquals("1.7B", Money.compact(172_000_000_000L));
    }
}
