package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.parse.ItemKeyFactory;
import dev.hugomarket.store.AhFocus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JumpCheckTest {
    private static Listing listing(long price, int count) {
        ItemView item = new ItemView("minecraft:firework_rocket", count, "Feuerwerksrakete", List.of(), Map.of(), Map.of(), null, null, List.of(), null, null, Map.of(), List.of());
        return new Listing(0, item, ItemKeyFactory.of(item), price, "seller", 3600, ListingStatus.ACTIVE, false);
    }

    private static List<Listing> page(int size, long from) {
        List<Listing> out = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            out.add(listing(from + i * 100, 64));
        }
        return out;
    }

    @Test
    void findsMatchingOffer() {
        assertEquals(JumpCheck.Outcome.FOUND, JumpCheck.check(page(36, 2_000), 36, new AhFocus("firework_rocket", 2_300, 64, "x")));
    }

    @Test
    void cheaperThanPageButMissingIsGone() {
        assertEquals(JumpCheck.Outcome.GONE, JumpCheck.check(page(36, 2_000), 36, new AhFocus("firework_rocket", 2_050, 64, "x")));
        assertEquals(JumpCheck.Outcome.GONE, JumpCheck.check(page(5, 2_000), 36, new AhFocus("firework_rocket", 99_999, 64, "x")));
    }

    @Test
    void pricierThanFullPageIsFurther() {
        assertEquals(JumpCheck.Outcome.FURTHER, JumpCheck.check(page(36, 2_000), 36, new AhFocus("firework_rocket", 99_999, 64, "x")));
    }
}
