package dev.hugomarket;

import dev.hugomarket.config.ConfigStore;
import dev.hugomarket.config.MenuScreen;
import dev.hugomarket.container.ContainerTracker;
import dev.hugomarket.container.ScreenStateTracker;
import dev.hugomarket.core.ClientScheduler;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.core.ModuleRegistry;
import dev.hugomarket.core.ServerDetector;
import dev.hugomarket.module.ahsearch.AhSearchModule;
import dev.hugomarket.module.calc.CalcModule;
import dev.hugomarket.module.containervalue.ContainerValueModule;
import dev.hugomarket.module.dumper.DumperModule;
import dev.hugomarket.module.orders.OrdersModule;
import dev.hugomarket.module.sellassist.SellAssistModule;
import dev.hugomarket.module.sellstats.SellStatsModule;
import dev.hugomarket.module.sync.SyncModule;
import dev.hugomarket.module.tooltips.TooltipsModule;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.parse.ScreenClassifier;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.PriceStore;
import dev.hugomarket.store.UserData;
import dev.hugomarket.ui.KeyBindings;
import dev.hugomarket.ui.OverlayManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;

public final class HugoMarketClient implements ClientModInitializer {
    private static ModuleRegistry registry;

    public static ModuleRegistry registry() {
        return registry;
    }

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
        PriceStore priceStore = new PriceStore(dataDir.resolve("price-cache.json"));
        priceStore.load();
        MarketSession market = new MarketSession(priceStore);
        UserData userData = new UserData(dataDir.resolve("templates.json"));
        userData.load();
        ModuleContext context = new ModuleContext(config, scheduler, server, containers, screens, guiRules, overlays, market, userData, dataDir);
        registry = new ModuleRegistry(context, List.of(new DumperModule(), new AhSearchModule(), new SellAssistModule(),
                new TooltipsModule(), new ContainerValueModule(), new CalcModule(), new OrdersModule(), new SellStatsModule(),
                new SyncModule()));
        registry.start();
        KeyMapping menuKey = KeyBindings.register("key.hugomarket.menu", GLFW.GLFW_KEY_UNKNOWN);
        scheduler.onTick(client -> {
            while (menuKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new MenuScreen(null, registry));
                }
            }
        });
    }
}
