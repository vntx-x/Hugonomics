package dev.hugomarket.parse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HugoLoreTest {
    @Test
    void parsesListingLines() {
        List<String> lore = List.of("", "Preis: $23.323", "Verkäufer: Teufels_Affe_YT", "Läuft ab in: 10h 43m 26s");
        assertEquals(2_332_300, HugoLore.price(lore).getAsLong());
        assertEquals("Teufels_Affe_YT", HugoLore.seller(lore).orElseThrow());
        assertEquals(10 * 3600 + 43 * 60 + 26, HugoLore.expiresInSeconds(lore).getAsLong());
    }

    @Test
    void parsesWorthWithAndWithoutBonus() {
        HugoLore.Worth bonus = HugoLore.worth(List.of("Wert: $42,24 (+$3,84 durch 1.1x)")).orElseThrow();
        assertEquals(4224, bonus.valueCents());
        assertEquals(384, bonus.bonusCents());
        assertEquals(1.1, bonus.multiplier(), 1e-9);
        assertEquals(3840, bonus.baseCents());

        HugoLore.Worth plain = HugoLore.worth(List.of("Wert: $4")).orElseThrow();
        assertEquals(400, plain.valueCents());
        assertEquals(0, plain.bonusCents());
        assertEquals(1.0, plain.multiplier(), 1e-9);
    }

    @Test
    void parsesOrderLines() {
        List<String> lore = List.of("Elytren Reparatur 1 Haltbarkeit 3", "Preis pro Stück: $1.720.000.000", "",
                "0/1 Geliefert", "$0/$1.720.000.000 Ausgezahlt", "", "Klicke, um X Elytren zu liefern");
        assertEquals(172_000_000_000L, HugoLore.unitPrice(lore).getAsLong());
        assertEquals(new HugoLore.Progress(0, 1), HugoLore.delivered(lore).orElseThrow());
        assertEquals(new HugoLore.PaidOut(0, 172_000_000_000L), HugoLore.paidOut(lore).orElseThrow());
    }

    @Test
    void parsesOrderCreationLines() {
        List<String> lore = List.of("Redstone-Staub", "Anzahl: 64", "Preis pro Stück: $100", "", "Einzahlung: $6.400",
                "Erstellgebühr: $64", "Gesamtzahlung: $6.464");
        assertEquals(64, HugoLore.amount(lore).getAsLong());
        assertEquals(6400, HugoLore.fee(lore).getAsLong());
    }

    @Test
    void findsSelectedOptionsPerSection() {
        List<String> lore = List.of("Sortierung:", "Neueste Auktionen", "Älteste Auktionen", "> Niedrigster Preis",
                "Höchster Preis", "Stack-Filter:", "> Alle", "Nur ganze Stacks", "Nur einzelne Items");
        assertEquals("Niedrigster Preis", HugoLore.selectedOption(lore, "Sortierung:").orElseThrow());
        assertEquals("Alle", HugoLore.selectedOption(lore, "Stack-Filter:").orElseThrow());
        assertTrue(HugoLore.selectedOption(lore, "Unbekannt:").isEmpty());
    }

    @Test
    void parsesTotals() {
        List<String> lore = List.of("> Alle Auktionen anzeigen", "Insgesamt ausgegeben: $7.454.495", "Insgesamt verdient: $6.972.083");
        assertEquals(745_449_500L, HugoLore.spentTotal(lore).getAsLong());
        assertEquals(697_208_300L, HugoLore.earnedTotal(lore).getAsLong());
    }
}
