package dev.hugomarket.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDataAlertsTest {
    @Test
    void alertsPersistAndClear(@TempDir Path dir) {
        UserData data = new UserData(dir.resolve("templates.json"));
        data.setAlert("nether_star", 75_000_000);
        UserData reloaded = new UserData(dir.resolve("templates.json"));
        reloaded.load();
        assertEquals(75_000_000, reloaded.alert("nether_star").getAsLong());
        assertEquals(1, reloaded.alerts().size());
        reloaded.setAlert("nether_star", 0);
        assertTrue(reloaded.alert("nether_star").isEmpty());
    }
}
