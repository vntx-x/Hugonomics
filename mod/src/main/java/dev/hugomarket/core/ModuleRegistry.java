package dev.hugomarket.core;

import dev.hugomarket.HugoMarket;

import java.util.ArrayList;
import java.util.List;

public final class ModuleRegistry {
    private final ModuleContext context;
    private final List<Module> modules;
    private final List<Module> enabled = new ArrayList<>();

    public ModuleRegistry(ModuleContext context, List<Module> modules) {
        this.context = context;
        this.modules = List.copyOf(modules);
    }

    public void start() {
        for (Module module : modules) {
            if (!context.config().get().isModuleEnabled(module.id(), module.enabledByDefault())) {
                continue;
            }
            module.onEnable(context);
            enabled.add(module);
            HugoMarket.LOGGER.info("module {} enabled", module.id());
        }
    }

    public void stop() {
        for (Module module : enabled) {
            module.onDisable();
        }
        enabled.clear();
    }
}
