package dev.hugomarket.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hugomarket.HugoMarket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path file;
    private ModConfig config;

    private ConfigStore(Path file, ModConfig config) {
        this.file = file;
        this.config = config;
    }

    public static ConfigStore load(Path file) {
        ModConfig config = null;
        if (Files.exists(file)) {
            try {
                config = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), ModConfig.class);
            } catch (IOException | RuntimeException e) {
                HugoMarket.LOGGER.warn("config unreadable, using defaults: {}", e.toString());
            }
        }
        ConfigStore store = new ConfigStore(file, config == null ? new ModConfig() : config);
        if (!Files.exists(file)) {
            store.save();
        }
        return store;
    }

    public ModConfig get() {
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(config), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            HugoMarket.LOGGER.error("config save failed", e);
        }
    }
}
