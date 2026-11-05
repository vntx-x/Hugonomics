package dev.hugomarket.module.sync;

import dev.hugomarket.config.ModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SyncClientIdTest {
    private static ModConfig config(String monitorAccount) {
        ModConfig config = new ModConfig();
        config.clientId = "590bc566-031c-44cb-b624-213d9580d920";
        config.monitorAccount = monitorAccount;
        return config;
    }

    @Test
    void mainAccountKeepsConfiguredId() {
        assertEquals("590bc566-031c-44cb-b624-213d9580d920", SyncModule.clientIdFor(config("KOVAAV"), "VNTX_X"));
        assertEquals("590bc566-031c-44cb-b624-213d9580d920", SyncModule.clientIdFor(config(""), "KOVAAV"));
    }

    @Test
    void monitorAccountGetsStableDerivedId() {
        String derived = SyncModule.clientIdFor(config("KOVAAV"), "KovaaV");
        assertNotEquals("590bc566-031c-44cb-b624-213d9580d920", derived);
        assertEquals(derived, SyncModule.clientIdFor(config("kovaav"), "KOVAAV"));
        assertEquals(36, derived.length());
    }
}
