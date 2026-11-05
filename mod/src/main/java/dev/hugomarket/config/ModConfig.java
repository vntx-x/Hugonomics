package dev.hugomarket.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModConfig {
    public Map<String, Boolean> modules = new HashMap<>();
    public boolean debugAnyServer = false;
    public String backendUrl = "https://hugomarket.duckdns.org";
    public String clientId = null;
    public boolean tutorialShown = false;
    public boolean calculatorOpen = true;
    public int monitorIntervalSeconds = 45;
    public String monitorAccount = "";
    public boolean monitorHeadless = false;
    public List<String> monitorQueries = new ArrayList<>();

    public boolean isModuleEnabled(String id, boolean fallback) {
        Boolean value = modules.get(id);
        return value == null ? fallback : value;
    }
}
