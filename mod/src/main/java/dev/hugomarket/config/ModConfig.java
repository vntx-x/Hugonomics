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
    public boolean spawnerExchangeJoined = false;
    public double hudX = 0;
    public double hudY = 0;
    public int monitorIntervalSeconds = 45;
    public int monitorRefreshSeconds = 3;
    public int monitorRefreshMaxSeconds = 7;
    public int monitorSweepEvery = 0;
    public String monitorAccount = "";
    public boolean monitorHeadless = false;
    public boolean monitorSneak = true;
    public List<String> monitorQueries = new ArrayList<>();

    public boolean isModuleEnabled(String id, boolean fallback) {
        Boolean value = modules.get(id);
        return value == null ? fallback : value;
    }
}
