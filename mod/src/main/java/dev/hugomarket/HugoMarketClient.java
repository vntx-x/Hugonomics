package dev.hugomarket;

import dev.hugomarket.config.ConfigStore;
import dev.hugomarket.container.ContainerTracker;
import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.core.ModuleRegistry;
import dev.hugomarket.core.ServerDetector;
import dev.hugomarket.module.dumper.DumperModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.List;

public final class HugoMarketClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Path dataDir = FabricLoader.getInstance().getConfigDir().resolve(HugoMarket.MOD_ID);
        ConfigStore config = ConfigStore.load(dataDir.resolve("config.json"));
        ClientScheduler scheduler = new ClientScheduler();
        ServerDetector server = new ServerDetector(() -> config.get().debugAnyServer);
        ContainerTracker containers = new ContainerTracker(server, scheduler);
        ModuleContext context = new ModuleContext(config, scheduler, server, containers, dataDir);
        new ModuleRegistry(context, List.of(new DumperModule())).start();
    }
}
