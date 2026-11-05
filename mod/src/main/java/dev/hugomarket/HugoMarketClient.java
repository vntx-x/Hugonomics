package dev.hugomarket;

import dev.hugomarket.config.ConfigStore;
import dev.hugomarket.container.ContainerTracker;
import dev.hugomarket.container.ScreenStateTracker;
import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.core.ModuleRegistry;
import dev.hugomarket.core.ServerDetector;
import dev.hugomarket.module.ahsearch.AhSearchModule;
import dev.hugomarket.module.dumper.DumperModule;
import dev.hugomarket.module.sellassist.SellAssistModule;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.parse.ScreenClassifier;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.ui.OverlayManager;
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
        HugoGuiRules guiRules = HugoGuiRules.load();
        ScreenStateTracker screens = new ScreenStateTracker(new ScreenClassifier(guiRules), containers);
        OverlayManager overlays = new OverlayManager();
        MarketSession market = new MarketSession();
        ModuleContext context = new ModuleContext(config, scheduler, server, containers, screens, guiRules, overlays, market, dataDir);
        new ModuleRegistry(context, List.of(new DumperModule(), new AhSearchModule(), new SellAssistModule())).start();
    }
}
