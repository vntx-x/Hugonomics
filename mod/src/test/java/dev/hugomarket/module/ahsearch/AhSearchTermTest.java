package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.parse.ItemKeyFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AhSearchTermTest {
    private static Listing listing(String id, String name, String model, Map<String, Integer> stored) {
        ItemView item = new ItemView(id, 1, name, List.of(), Map.of(), stored, null, model, List.of(), null, model == null ? null : name, Map.of(), List.of());
        return new Listing(0, item, ItemKeyFactory.of(item), 100, "seller", 3600, ListingStatus.ACTIVE, false);
    }

    @Test
    void vanillaUsesItemId() {
        assertEquals("golden apple", AhSearchTerm.of(listing("minecraft:golden_apple", "Goldener Apfel", null, Map.of())));
        assertEquals("enchanted book", AhSearchTerm.of(listing("minecraft:enchanted_book", "Verzaubertes Buch", null, Map.of("minecraft:mending", 1))));
    }

    @Test
    void customItemsUseCleanDisplayName() {
        assertEquals("star pickaxe", AhSearchTerm.of(listing("minecraft:diamond_pickaxe", "§6Star Pickaxe ★",
                "hugosmp:668dbfbac74c/0ce8607e06cf/item/4e/90/8d5ccafc14557d1e", Map.of())));
    }
}
