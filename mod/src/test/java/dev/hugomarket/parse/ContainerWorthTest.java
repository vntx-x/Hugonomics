package dev.hugomarket.parse;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.SlotView;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerWorthTest {
    @Test
    void chestWorthLinesParseForEveryPricedItem() {
        ContainerView chest = DumpLoader.container("chest/chest-1.json");
        long priced = chest.containerItems().filter(s -> HugoLore.worth(s.item().lore()).isPresent()).count();
        long withLine = chest.containerItems().filter(s -> s.item().lore().stream().anyMatch(l -> l.startsWith("Wert:"))).count();
        assertEquals(withLine, priced);
        assertTrue(priced >= 15);

        SlotView gravel = chest.slot(0);
        HugoLore.Worth worth = HugoLore.worth(gravel.item().lore()).orElseThrow();
        assertEquals(4224, worth.valueCents());
        assertEquals(384, worth.bonusCents());
    }

    @Test
    void enderchestShulkersExposeContents() {
        ContainerView enderchest = DumpLoader.container("enderchest/enderchest-1.json");
        SlotView shulker = enderchest.containerItems().filter(s -> s.item().id().equals("minecraft:shulker_box")).findFirst().orElseThrow();
        assertTrue(shulker.item().hasContents());
        assertEquals(27, shulker.item().contents().size());
        assertEquals("minecraft:bone", shulker.item().contents().get(0).id());
        assertEquals(64, shulker.item().contents().get(0).count());
    }
}
