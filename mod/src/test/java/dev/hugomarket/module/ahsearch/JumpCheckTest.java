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
            out.add(listing(from + i * 100L, 64));
        }
        return out;
    }

    private static AhFocus focus(long price) {
        return new AhFocus("firework_rocket", price, 64, "x");
    }

    @Test
    void findsMatchingOffer() {
        assertEquals(JumpCheck.Outcome.FOUND, JumpCheck.check(page(36, 2_000), true, focus(2_300)));
    }

    @Test
    void pricierThanPageGoesToNextPage() {
        assertEquals(JumpCheck.Outcome.NEXT_PAGE, JumpCheck.check(page(36, 2_000), true, focus(99_999)));
    }

    @Test
    void goneWhenPassedOrNoMorePages() {
        assertEquals(JumpCheck.Outcome.GONE, JumpCheck.check(page(36, 5_000), true, focus(2_050)));
        assertEquals(JumpCheck.Outcome.GONE, JumpCheck.check(page(10, 2_000), false, focus(99_999)));
        assertEquals(JumpCheck.Outcome.GONE, JumpCheck.check(List.of(), true, focus(2_050)));
    }

    @Test
    void cheaperButNotYetPassedKeepsLooking() {
        assertEquals(JumpCheck.Outcome.NEXT_PAGE, JumpCheck.check(page(36, 2_000), true, focus(2_050)));
    }
}
