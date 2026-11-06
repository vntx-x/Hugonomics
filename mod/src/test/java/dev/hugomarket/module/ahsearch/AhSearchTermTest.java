package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.parse.ItemKeyFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AhSearchTermTest {
    private static final Function<String, String> GERMAN = id -> switch (id) {
        case "minecraft:mace" -> "Streitkolben";
        case "minecraft:golden_apple" -> "Goldener Apfel";
        default -> null;
    };

    private static Listing listing(String id, String name, String model) {
        ItemView item = new ItemView(id, 1, name, List.of(), Map.of(), Map.of(), null, model, List.of(), null, model == null ? null : name, Map.of(), List.of());
        return new Listing(0, item, ItemKeyFactory.of(item), 100, "seller", 3600, ListingStatus.ACTIVE, false);
    }

    @Test
    void vanillaUsesGermanName() {
        assertEquals("streitkolben", AhSearchTerm.of(listing("minecraft:mace", "Mace", null), GERMAN));
        assertEquals("goldener apfel", AhSearchTerm.of(listing("minecraft:golden_apple", "Golden Apple", null), GERMAN));
    }

    @Test
    void fallsBackToItemId() {
        assertEquals("heavy core", AhSearchTerm.of(listing("minecraft:heavy_core", "Heavy Core", null), GERMAN));
    }

    @Test
    void customItemsUseCleanDisplayName() {
        assertEquals("star pickaxe", AhSearchTerm.of(listing("minecraft:diamond_pickaxe", "§6Star Pickaxe ★",
                "hugosmp:668dbfbac74c/0ce8607e06cf/item/4e/90/8d5ccafc14557d1e"), GERMAN));
    }
}
