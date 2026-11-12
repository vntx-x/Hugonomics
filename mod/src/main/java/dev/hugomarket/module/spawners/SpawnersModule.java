package dev.hugomarket.module.spawners;

import dev.hugomarket.HugoMarket;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.store.SpawnerPrices;
import net.minecraft.client.Minecraft;

public final class SpawnersModule implements Module {
    private static final long REFRESH_MS = 30 * 60_000;

    private ModuleContext context;
    private long loadedAt;
    private boolean loading;

    @Override
    public String id() {
        return "spawners";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        context.menu().add("hugomarket.spawners.open", this::open);
        load();
    }

    @Override
    public void onDisable() {
        context.market().setSpawnerPrices(SpawnerPrices.EMPTY);
        loadedAt = 0;
    }

    private void load() {
        if (!context.hasBackend() || loading || System.currentTimeMillis() - loadedAt < REFRESH_MS) {
            return;
        }
        loading = true;
        context.backend().get("/v1/spawners").whenComplete((json, error) -> context.scheduler().post(() -> {
            loading = false;
            if (error != null) {
                HugoMarket.LOGGER.warn("spawner prices unavailable: {}", error.toString());
                return;
            }
            loadedAt = System.currentTimeMillis();
            context.market().setSpawnerPrices(SpawnerPrices.parse(json));
        }));
    }

    private void open() {
        load();
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new SpawnersScreen(client.screen, context.market(), context.hasBackend()));
    }
}
