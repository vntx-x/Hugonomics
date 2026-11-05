package dev.hugomarket.module.networth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceParserTest {
    @Test
    void prefersLabeledSidebarLine() {
        List<String> lines = List.of("HugoSMP", "Kills: 12", "Bounty: $500", "Geld: $1.234.567,50", "Rang: Pro");
        assertEquals(123_456_750L, BalanceParser.fromSidebar(lines).getAsLong());
    }

    @Test
    void readsHugoSidebarWithSuffix() {
        List<String> lines = List.of("HugoSMP", " Geld 2,73M 14", " Gems 950 13", "Kills 61 12", "Spielzeit 12d 12h 8");
        assertEquals(2_730_000_00L, BalanceParser.fromSidebar(lines).getAsLong());
        assertEquals(45_500_00L, BalanceParser.fromSidebar(List.of("Geld 45,5K")).getAsLong());
        assertEquals(1_200_00L, BalanceParser.fromSidebar(List.of("Geld 1.200 3")).getAsLong());
        assertEquals(2_000_000_000_00L, BalanceParser.fromSidebar(List.of("Geld 2B")).getAsLong());
    }

    @Test
    void fallsBackToAnyDollarAmount() {
        assertEquals(42_00L, BalanceParser.fromSidebar(List.of("Server", "» $42", "Online: 120")).getAsLong());
        assertEquals(1_720_000_000_00L, BalanceParser.fromSidebar(List.of("1.720.000.000$")).getAsLong());
    }

    @Test
    void ignoresSidebarWithoutMoney() {
        assertTrue(BalanceParser.fromSidebar(List.of("HugoSMP", "Kills: 12", "Online: 120")).isEmpty());
    }

    @Test
    void readsBalanceCommandOutputOnly() {
        assertEquals(9_876_00L, BalanceParser.fromMessage("Dein Kontostand: $9.876").getAsLong());
        assertTrue(BalanceParser.fromMessage("[HugoSMP] +$159,16 (Basis: $144,80, Bonus: +$14,36 durch 1.1x)").isEmpty());
        assertTrue(BalanceParser.fromMessage("Preis: $5.000").isEmpty());
        assertTrue(BalanceParser.fromMessage("Geld überwiesen: -$500").isEmpty());
    }
}
