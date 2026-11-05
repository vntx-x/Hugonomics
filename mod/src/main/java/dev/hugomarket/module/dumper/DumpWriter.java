package dev.hugomarket.module.dumper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.hugomarket.HugoMarket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DumpWriter {
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    private final Path containerDir;
    private final Path chatDir;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "hugomarket-dump-writer");
        thread.setDaemon(true);
        return thread;
    });

    public DumpWriter(Path root) {
        this.containerDir = root.resolve("containers");
        this.chatDir = root.resolve("chat");
    }

    public void writeContainer(String fileName, JsonObject json) {
        String text = PRETTY.toJson(json);
        executor.execute(() -> {
            try {
                Files.createDirectories(containerDir);
                Files.writeString(containerDir.resolve(fileName), text, StandardCharsets.UTF_8);
            } catch (IOException e) {
                HugoMarket.LOGGER.error("container dump failed: {}", fileName, e);
            }
        });
    }

    public void appendChat(String fileName, JsonObject json) {
        String line = COMPACT.toJson(json) + "\n";
        executor.execute(() -> {
            try {
                Files.createDirectories(chatDir);
                Files.writeString(chatDir.resolve(fileName), line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                HugoMarket.LOGGER.error("chat dump failed: {}", fileName, e);
            }
        });
    }

    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
