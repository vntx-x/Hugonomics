package dev.hugomarket.config;

import java.util.HashMap;
import java.util.Map;

public final class ModConfig {
    public Map<String, Boolean> modules = new HashMap<>();
    public boolean debugAnyServer = false;
    public String backendUrl = "https://hugomarket.duckdns.org";
    public String clientId = null;
    public boolean tutorialShown = false;

    public boolean isModuleEnabled(String id, boolean fallback) {
        Boolean value = modules.get(id);
        return value == null ? fallback : value;
    }
}
