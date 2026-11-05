package dev.hugomarket.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hugomarket.HugoMarket;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class BackendClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final int FAILURES_BEFORE_PAUSE = 3;
    private static final long PAUSE_MS = 60_000;

    private final String baseUrl;
    private final String userAgent;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread thread = new Thread(r, "hugomarket-backend");
        thread.setDaemon(true);
        return thread;
    });
    private final HttpClient http;
    private final AtomicInteger failures = new AtomicInteger();
    private final AtomicLong pausedUntil = new AtomicLong();

    public BackendClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.userAgent = HugoMarket.MOD_ID + "/" + HugoMarket.modVersion();
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).executor(executor).build();
    }

    public boolean available() {
        return System.currentTimeMillis() >= pausedUntil.get();
    }

    public CompletableFuture<JsonObject> get(String path) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET());
    }

    public CompletableFuture<JsonObject> post(String path, JsonElement body) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)));
    }

    private CompletableFuture<JsonObject> send(HttpRequest.Builder builder) {
        if (!available()) {
            return CompletableFuture.failedFuture(new IllegalStateException("backend paused"));
        }
        HttpRequest request = builder.timeout(TIMEOUT).header("User-Agent", userAgent).build();
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("http " + response.statusCode());
                    }
                    failures.set(0);
                    return JsonParser.parseString(response.body()).getAsJsonObject();
                })
                .whenComplete((result, error) -> {
                    if (error != null && failures.incrementAndGet() >= FAILURES_BEFORE_PAUSE) {
                        failures.set(0);
                        pausedUntil.set(System.currentTimeMillis() + PAUSE_MS);
                        HugoMarket.LOGGER.warn("backend paused for 60 s after repeated failures: {}", error.toString());
                    }
                });
    }

    public void close() {
        executor.shutdownNow();
    }
}
