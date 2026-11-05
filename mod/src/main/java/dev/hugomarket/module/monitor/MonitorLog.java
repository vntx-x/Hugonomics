package dev.hugomarket.module.monitor;

import dev.hugomarket.HugoMarket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

final class MonitorLog {
    private static final long MAX_BYTES = 1_000_000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Path file;

    MonitorLog(Path file) {
        this.file = file;
    }

    void line(String text) {
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file) && Files.size(file) > MAX_BYTES) {
                Files.delete(file);
            }
            Files.writeString(file, TIME.format(LocalTime.now()) + " " + text + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            HugoMarket.LOGGER.warn("monitor log failed: {}", e.toString());
        }
    }
}
