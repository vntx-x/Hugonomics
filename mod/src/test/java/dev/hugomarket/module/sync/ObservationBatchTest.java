package dev.hugomarket.module.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.testsupport.DumpLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationBatchTest {
    private final AhListingParser parser = new AhListingParser(HugoGuiRules.load());

    @Test
    void hashesSellersAndKeepsSchemaFields() {
        List<Listing> listings = parser.parse(DumpLoader.container("ah-list/ah-list-2.json")).listings();
        JsonObject batch = ObservationBatch.build(listings, "11111111-1111-4111-8111-111111111111", "0.0.1", "1.21.5",
                1_762_344_000_000L, "pepper");
        assertEquals("ah_list", batch.get("source").getAsString());
        assertEquals(java.time.Instant.ofEpochMilli(1_762_344_000_000L).toString(), batch.get("observedAt").getAsString());
        assertTrue(batch.get("observedAt").getAsString().endsWith("Z"));
        JsonArray array = batch.getAsJsonArray("listings");
        assertEquals(listings.size(), array.size());
        JsonObject first = array.get(0).getAsJsonObject();
        assertEquals("nether_star", first.get("key").getAsString());
        assertEquals("minecraft:nether_star", first.get("baseId").getAsString());
        assertEquals(80_000_000, first.get("priceCents").getAsLong());
        assertEquals(32, first.get("sellerHash").getAsString().length());
        assertTrue(first.get("sellerHash").getAsString().matches("[a-f0-9]+"));
        assertFalse(batch.toString().contains(listings.get(0).seller()));
    }

    @Test
    void ownListingsAreNeverUploaded() {
        List<Listing> own = parser.parse(DumpLoader.container("ah-own-laufend/ah-own-laufend-1.json")).listings();
        JsonObject batch = ObservationBatch.build(own, "11111111-1111-4111-8111-111111111111", "0.0.1", "1.21.5", 0, "p");
        assertEquals(0, batch.getAsJsonArray("listings").size());
    }

    @Test
    void blankDisplayNameFallsBackToKey() {
        Listing real = parser.parse(DumpLoader.container("ah-list/ah-list-2.json")).listings().get(0);
        ItemView i = real.item();
        ItemView unnamed = new ItemView(i.id(), i.count(), " ", i.lore(), i.enchantments(), i.storedEnchantments(), i.potion(),
                i.itemModel(), i.customModelData(), i.spawnerEntity(), i.customName(), i.bukkitValues(), i.contents());
        Listing listing = new Listing(real.slot(), unnamed, real.key(), real.priceCents(), real.seller(),
                real.expiresInSeconds(), real.status(), real.own());
        JsonObject batch = ObservationBatch.build(List.of(listing), "11111111-1111-4111-8111-111111111111", "0.0.1", "1.21.5", 0, "p");
        assertEquals("nether_star", batch.getAsJsonArray("listings").get(0).getAsJsonObject().get("displayName").getAsString());
    }

    @Test
    void sellerHashIsCaseInsensitiveAndPeppered() {
        assertEquals(SellerHash.of("Player", "p"), SellerHash.of("player", "p"));
        assertFalse(SellerHash.of("player", "p1").equals(SellerHash.of("player", "p2")));
    }

    @Test
    void versionComparison() {
        assertTrue(VersionCompare.atLeast("0.0.1+1.21.5", "0.0.1"));
        assertTrue(VersionCompare.atLeast("0.1.0", "0.0.9"));
        assertFalse(VersionCompare.atLeast("0.0.1", "0.0.2"));
        assertTrue(VersionCompare.atLeast("1.0", "0.9.9"));
    }
}
