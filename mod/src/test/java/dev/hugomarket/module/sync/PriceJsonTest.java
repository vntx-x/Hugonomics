package dev.hugomarket.module.sync;

import com.google.gson.JsonParser;
import dev.hugomarket.store.PriceStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceJsonTest {
    @Test
    void parsesPriceStats() {
        PriceStats stats = PriceJson.stats(JsonParser.parseString(
                "{\"key\":\"emerald\",\"lowestUnitCents\":100,\"medianUnitCents\":120,\"listings\":5,\"units\":40,\"lastSeen\":123,\"confirmed\":true}").getAsJsonObject());
        assertEquals(100, stats.lowestUnitCents());
        assertEquals(120, stats.medianUnitCents());
        assertEquals(5, stats.listings());
        assertEquals(40, stats.units());
        assertEquals(123, stats.lastSeen());
    }

    @Test
    void parsesItemsAndHistory() {
        List<PriceJson.ItemSummary> items = PriceJson.items(JsonParser.parseString(
                "{\"serverTime\":1,\"items\":[{\"key\":\"emerald\",\"baseId\":\"minecraft:emerald\",\"displayName\":\"Emerald\",\"activeListings\":3,\"lastSeen\":9}]}").getAsJsonObject());
        assertEquals(1, items.size());
        assertEquals("minecraft:emerald", items.get(0).baseId());
        assertEquals(3, items.get(0).activeListings());

        List<PriceJson.HistoryPoint> points = PriceJson.history(JsonParser.parseString(
                "{\"key\":\"emerald\",\"points\":[{\"bucketTs\":1000,\"lowestUnitCents\":100,\"medianUnitCents\":110,\"listings\":2,\"units\":2,\"confirmed\":false}]}").getAsJsonObject());
        assertEquals(1, points.size());
        assertEquals(1000, points.get(0).ts());
        assertEquals(110, points.get(0).medianUnitCents());
        assertTrue(PriceJson.items(JsonParser.parseString("{}").getAsJsonObject()).isEmpty());
    }
}
