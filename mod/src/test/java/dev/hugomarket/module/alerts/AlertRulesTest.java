package dev.hugomarket.module.alerts;

import dev.hugomarket.model.Listing;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertRulesTest {
    private final List<Listing> page = new AhListingParser(HugoGuiRules.load()).parse(DumpLoader.container("ah-list/ah-list-2.json")).listings();

    private List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < page.size(); i++) {
            ids.add("id" + i);
        }
        return ids;
    }

    @Test
    void firesForListingsAtOrBelowAlertAndRespectsCooldown() {
        AlertRules rules = new AlertRules();
        Map<String, Long> alerts = Map.of("nether_star", 80_000_000L);
        List<AlertRules.Hit> hits = rules.check(page, ids(), alerts, 1_000);
        long stars = page.stream().filter(l -> l.key().toString().equals("nether_star")).count();
        assertEquals(stars, hits.size());
        assertTrue(hits.stream().allMatch(h -> h.listing().unitPriceCents() <= 80_000_000));

        assertEquals(0, rules.check(page, ids(), alerts, 2_000).size());
        assertEquals(stars, rules.check(page, ids(), alerts, 1_000 + 11 * 60_000).size());
    }

    @Test
    void ignoresHigherPricesAndUnknownKeys() {
        AlertRules rules = new AlertRules();
        assertEquals(0, rules.check(page, ids(), Map.of("nether_star", 79_999_999L), 1_000).size());
        assertEquals(0, rules.check(page, ids(), Map.of("dragon_egg", 1L), 1_000).size());
    }
}
