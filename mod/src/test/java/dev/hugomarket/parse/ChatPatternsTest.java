package dev.hugomarket.parse;

import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPatternsTest {
    @Test
    void parsesPayoutWithBonus() {
        ChatPatterns.Payout payout = ChatPatterns.payout("[HugoSMP] +$159,16 (Basis: $144,80, Bonus: +$14,36 durch 1.1x)").orElseThrow();
        assertEquals(15916, payout.totalCents());
        assertEquals(14480, payout.baseCents());
        assertEquals(1436, payout.bonusCents());
        assertEquals(1.1, payout.multiplier(), 1e-9);
    }

    @Test
    void parsesPayoutWithoutBonus() {
        ChatPatterns.Payout payout = ChatPatterns.payout("[HugoSMP] +$4").orElseThrow();
        assertEquals(400, payout.totalCents());
        assertEquals(400, payout.baseCents());
        assertEquals(0, payout.bonusCents());
    }

    @Test
    void recognisesSystemMessages() {
        assertTrue(ChatPatterns.isAuctionBought("[HugoSMP] Du hast die Auktion erfolgreich gekauft!"));
        assertTrue(ChatPatterns.isOrderCreated("[HugoSMP] Order erstellt."));
        assertFalse(ChatPatterns.isOrderCreated("[HugoSMP] Befehl nicht gefunden."));
        assertTrue(ChatPatterns.payout("[HugoSMP] Befehl nicht gefunden.").isEmpty());
    }

    @Test
    void matchesRecordedServerLines() {
        List<String> lines = DumpLoader.chatLines("chat/hugosmp-system.jsonl");
        assertEquals(1, lines.stream().filter(l -> ChatPatterns.payout(l).isPresent()).count());
        assertEquals(1, lines.stream().filter(ChatPatterns::isAuctionBought).count());
        assertEquals(1, lines.stream().filter(ChatPatterns::isOrderCreated).count());
    }
}
