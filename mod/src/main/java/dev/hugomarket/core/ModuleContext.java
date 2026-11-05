package dev.hugomarket.core;

import dev.hugomarket.config.ConfigStore;
import dev.hugomarket.container.ContainerTracker;
import dev.hugomarket.container.ScreenStateTracker;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.UserData;
import dev.hugomarket.ui.OverlayManager;

import java.nio.file.Path;

public record ModuleContext(
        ConfigStore config,
        ClientScheduler scheduler,
        ServerDetector server,
        ContainerTracker containers,
        ScreenStateTracker screens,
        HugoGuiRules guiRules,
        OverlayManager overlays,
        MarketSession market,
        UserData userData,
        Path dataDir
) {
}
