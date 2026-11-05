package dev.hugomarket.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinuteTestTest {
    @Test
    void countsOnlySecondsWithSalesAndFinishesAtSixty() {
        MinuteTest test = new MinuteTest();
        long t = 1_000_000;
        test.start(t);
        test.tick(t + 20_000);
        assertEquals(0, test.activeMs());
        for (int i = 0; i < 12; i++) {
            t += 5_000;
            test.sale(t, 1_000, 100, 1_100, 1.1);
        }
        assertTrue(test.isRunning());
        assertEquals(55_000, test.activeMs());
        test.tick(t + 5_000);
        assertFalse(test.isRunning());
        MinuteTest.Result result = test.finished();
        assertNotNull(result);
        assertEquals(12_000, result.baseCents());
        assertEquals(13_200, result.totalCents());
        assertEquals(12, result.sales());
        assertEquals(36_000, result.projected(3.0));
    }

    @Test
    void idleTimeBetweenSalesIsNotCounted() {
        MinuteTest test = new MinuteTest();
        long t = 1_000_000;
        test.start(t);
        test.sale(t + 1_000, 500, 0, 500, 1.0);
        test.tick(t + 120_000);
        assertTrue(test.isRunning());
        assertEquals(0, test.activeMs());
        test.cancel();
        assertFalse(test.isRunning());
        assertNull(test.finished());
    }

    @Test
    void savedTestsPersistWithNames(@TempDir Path dir) {
        MinuteTests tests = new MinuteTests(dir.resolve("minutetests.json"));
        MinuteTest.Result named = tests.add(new MinuteTest.Result("", 5, 1_000, 100, 1_100, 1.1, 3));
        assertEquals("Test 1", named.name());
        tests.rename(0, "Kaktus oben");
        MinuteTests reloaded = new MinuteTests(dir.resolve("minutetests.json"));
        reloaded.load();
        assertEquals(1, reloaded.all().size());
        assertEquals("Kaktus oben", reloaded.all().get(0).name());
        reloaded.remove(0);
        assertTrue(reloaded.all().isEmpty());
    }
}
