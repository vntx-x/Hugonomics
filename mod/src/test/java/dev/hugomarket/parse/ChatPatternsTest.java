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
    void readsAuctionSaleFromActionbar() {
        ChatPatterns.Sale sale = ChatPatterns.auctionSold("Du hast 64x Diamant für $5.120,50 verkauft!").orElseThrow();
        assertEquals(512_050, sale.priceCents());
        assertEquals(64, sale.count());
        assertEquals("Diamant", sale.item());
        ChatPatterns.Sale bare = ChatPatterns.auctionSold("Deine Auktion wurde verkauft: +$300").orElseThrow();
        assertEquals(30_000, bare.priceCents());
        assertEquals("", bare.item());
        assertTrue(ChatPatterns.auctionSold("[HugoSMP] Du hast die Auktion erfolgreich gekauft!").isEmpty());
        assertTrue(ChatPatterns.auctionSold("Spieler123: ich hab alles verkauft lol").isEmpty());
        ChatPatterns.Sale bought = ChatPatterns.auctionSold("Auktion gekauft: seller_1 hat 64x Spitzer Tropfstein für $20.000 gekauft.").orElseThrow();
        assertEquals(20_000_00L, bought.priceCents());
        assertEquals(64, bought.count());
        assertEquals("Spitzer Tropfstein", bought.item());
        assertEquals("seller_1", bought.buyer());
        assertEquals(1, ChatPatterns.auctionSold("Auktion gekauft: seller_2 hat Elytren für $1.500.000 gekauft.").orElseThrow().count());
        assertTrue(ChatPatterns.auctionSold("Spieler123: Auktion gekauft: lol").isEmpty());
    }

    @Test
    void matchesRecordedServerLines() {
        List<String> lines = DumpLoader.chatLines("chat/hugosmp-system.jsonl");
        assertEquals(1, lines.stream().filter(l -> ChatPatterns.payout(l).isPresent()).count());
        assertEquals(1, lines.stream().filter(ChatPatterns::isAuctionBought).count());
        assertEquals(1, lines.stream().filter(ChatPatterns::isOrderCreated).count());
    }
}
