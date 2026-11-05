package dev.hugomarket.core;

import dev.hugomarket.HugoMarket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleRegistry {
    private final ModuleContext context;
    private final List<Module> modules;
    private final Map<String, Boolean> running = new LinkedHashMap<>();

    public ModuleRegistry(ModuleContext context, List<Module> modules) {
        this.context = context;
        this.modules = List.copyOf(modules);
    }

    public void start() {
        for (Module module : modules) {
            boolean enabled = context.config().get().isModuleEnabled(module.id(), module.enabledByDefault());
            running.put(module.id(), false);
            if (enabled) {
                module.onEnable(context);
                running.put(module.id(), true);
                HugoMarket.LOGGER.info("module {} enabled", module.id());
            }
        }
    }

    public List<Module> modules() {
        return modules;
    }

    public boolean isEnabled(String id) {
        Module module = find(id);
        return module != null && context.config().get().isModuleEnabled(id, module.enabledByDefault());
    }

    public boolean isRunning(String id) {
        return Boolean.TRUE.equals(running.get(id));
    }

    public void setEnabled(String id, boolean enabled) {
        Module module = find(id);
        if (module == null) {
            return;
        }
        context.config().get().modules.put(id, enabled);
        context.config().save();
        if (!enabled && isRunning(id)) {
            module.onDisable();
            running.put(id, false);
        }
    }

    public void stop() {
        for (Module module : modules) {
            if (isRunning(module.id())) {
                module.onDisable();
            }
        }
        running.replaceAll((id, value) -> false);
    }

    private Module find(String id) {
        for (Module module : modules) {
            if (module.id().equals(id)) {
                return module;
            }
        }
        return null;
    }

    public List<String> ids() {
        List<String> ids = new ArrayList<>(modules.size());
        for (Module module : modules) {
            ids.add(module.id());
        }
        return ids;
    }
}
