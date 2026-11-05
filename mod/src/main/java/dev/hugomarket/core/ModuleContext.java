package dev.hugomarket.core;

import dev.hugomarket.config.ConfigStore;
import dev.hugomarket.container.ContainerTracker;

import java.nio.file.Path;

public record ModuleContext(
        ConfigStore config,
        ClientScheduler scheduler,
        ServerDetector server,
        ContainerTracker containers,
        Path dataDir
) {
}
