package dev.hugomarket.parse;

import dev.hugomarket.model.ItemKey;
import dev.hugomarket.model.ItemView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemKeyFactoryTest {
    private static ItemView item(String id, Map<String, Integer> enchantments, Map<String, Integer> stored, String potion,
                                 String model, List<Float> cmd, String spawner, String customName) {
        return new ItemView(id, 1, customName == null ? id : customName, List.of(), enchantments, stored, potion, model,
                cmd, spawner, customName, Map.of(), List.of());
    }

    @Test
    void sortsEnchantmentsAndStripsNamespace() {
        ItemKey key = ItemKeyFactory.of(item("minecraft:diamond_pickaxe",
                Map.of("minecraft:unbreaking", 3, "minecraft:efficiency", 5, "minecraft:mending", 1),
                Map.of(), null, null, List.of(), null, null));
        assertEquals("diamond_pickaxe#efficiency5;mending1;unbreaking3", key.toString());
        assertFalse(key.renamed());
    }

    @Test
    void usesStoredEnchantmentsForBooks() {
        ItemKey key = ItemKeyFactory.of(item("minecraft:enchanted_book", Map.of(), Map.of("minecraft:blast_protection", 1),
                null, null, List.of(), null, null));
        assertEquals("enchanted_book#blast_protection1", key.toString());
    }

    @Test
    void spawnerAndPotionBecomeVariants() {
        assertEquals("spawner#creeper", ItemKeyFactory.of(item("minecraft:spawner", Map.of(), Map.of(), null, null,
                List.of(), "minecraft:creeper", null)).toString());
        assertEquals("splash_potion#strong_strength", ItemKeyFactory.of(item("minecraft:splash_potion", Map.of(), Map.of(),
                "minecraft:strong_strength", null, List.of(), null, null)).toString());
    }

    @Test
    void customItemModelReplacesBase() {
        ItemKey key = ItemKeyFactory.of(item("minecraft:paper", Map.of(), Map.of(), null, "oraxen:ruby_sword", List.of(),
                null, "Rubinschwert"));
        assertEquals("oraxen:ruby_sword", key.toString());
        assertFalse(key.renamed());
    }

    @Test
    void renamedVanillaItemKeepsBaseAndFlags() {
        ItemKey key = ItemKeyFactory.of(item("minecraft:armadillo_scute", Map.of(), Map.of(), null, null, List.of(), null,
                "x_sucuk_x´s Haarschuppen"));
        assertEquals("armadillo_scute", key.toString());
        assertTrue(key.renamed());
    }

    @Test
    void customModelDataWithoutModelBecomesVariant() {
        ItemKey key = ItemKeyFactory.of(item("minecraft:paper", Map.of(), Map.of(), null, null, List.of(20005f), null, null));
        assertEquals("paper#cmd20005", key.toString());
    }

    @Test
    void parseRoundTrips() {
        assertEquals(new ItemKey("elytra", "mending1;unbreaking3", false), ItemKey.parse("elytra#mending1;unbreaking3"));
        assertEquals(new ItemKey("nether_star", "", false), ItemKey.parse("nether_star"));
    }
}
