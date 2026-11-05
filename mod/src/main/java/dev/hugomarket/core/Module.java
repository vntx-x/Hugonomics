package dev.hugomarket.core;

public interface Module {
    String id();

    boolean enabledByDefault();

    void onEnable(ModuleContext context);

    void onDisable();
}
