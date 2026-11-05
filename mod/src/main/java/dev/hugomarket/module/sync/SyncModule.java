package dev.hugomarket.module.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hugomarket.HugoMarket;
import dev.hugomarket.config.ModConfig;
import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.net.BackendClient;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.store.PriceStats;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SyncModule implements Module {
    private static final int UPLOAD_INTERVAL_TICKS = 20 * 30;
    private static final int PRICE_INTERVAL_TICKS = 20 * 15;
    private static final int SAVE_INTERVAL_TICKS = 20 * 60;
    private static final int MAX_QUEUE = 200;
    private static final int MAX_UPLOADS_PER_FLUSH = 5;
    private static final int MAX_KEYS_PER_REQUEST = 200;

    private ModuleContext context;
    private AhListingParser parser;
    private BackendClient client;
    private String clientId;
    private String pepper;
    private boolean uploadsAllowed;
    private boolean metaLoaded;
    private final Deque<JsonObject> queue = new ArrayDeque<>();
    private int ticks;
    private boolean uploading;
    private boolean fetching;

    @Override
    public String id() {
        return "sync";
    }

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        ModConfig config = context.config().get();
        if (config.backendUrl == null || config.backendUrl.isBlank()) {
            HugoMarket.LOGGER.info("sync enabled but backendUrl is empty, staying idle");
            return;
        }
        if (config.clientId == null || config.clientId.isBlank()) {
            config.clientId = UUID.randomUUID().toString();
            context.config().save();
        }
        clientId = config.clientId;
        parser = new AhListingParser(context.guiRules());
        client = new BackendClient(config.backendUrl);
        context.screens().onUpdate(this::onScreenUpdate);
        context.scheduler().onTick(mc -> tick());
        loadMeta();
    }

    @Override
    public void onDisable() {
        if (client != null) {
            client.close();
            client = null;
        }
        queue.clear();
        context.market().backend().saveIfDirty();
    }

    private void loadMeta() {
        client.get("/v1/meta").whenComplete((meta, error) -> context.scheduler().post(() -> {
            if (error != null) {
                HugoMarket.LOGGER.warn("backend meta unavailable: {}", error.toString());
                return;
            }
            pepper = meta.get("pepper").getAsString();
            String minVersion = meta.get("minClientVersion").getAsString();
            uploadsAllowed = VersionCompare.atLeast(HugoMarket.modVersion(), minVersion);
            metaLoaded = true;
            if (!uploadsAllowed) {
                HugoMarket.LOGGER.warn("mod version {} below backend minimum {}, uploads disabled", HugoMarket.modVersion(), minVersion);
            }
        }));
    }

    private void onScreenUpdate(ScreenUpdate update) {
        if (client == null || update.kind() != ScreenKind.AH_LIST || !metaLoaded || !uploadsAllowed) {
            return;
        }
        AhListingParser.AhPage page = parser.parse(update.view());
        if (!page.isMarket() || page.listings().isEmpty()) {
            return;
        }
        JsonObject batch = ObservationBatch.build(page.listings(), clientId, HugoMarket.modVersion(),
                HugoMarket.minecraftVersion(), update.snapshot().capturedAt(), pepper);
        if (batch.getAsJsonArray("listings").isEmpty()) {
            return;
        }
        if (queue.size() >= MAX_QUEUE) {
            queue.pollFirst();
        }
        queue.addLast(batch);
    }

    private void tick() {
        if (client == null) {
            return;
        }
        ticks++;
        if (!metaLoaded && ticks % UPLOAD_INTERVAL_TICKS == 0) {
            loadMeta();
        }
        if (ticks % UPLOAD_INTERVAL_TICKS == 0) {
            flushUploads();
        }
        if (ticks % PRICE_INTERVAL_TICKS == 0) {
            fetchPrices();
        }
        if (ticks % SAVE_INTERVAL_TICKS == 0) {
            context.market().backend().saveIfDirty();
        }
    }

    private void flushUploads() {
        if (uploading || queue.isEmpty() || !client.available() || !context.server().active()) {
            return;
        }
        uploading = true;
        uploadNext(MAX_UPLOADS_PER_FLUSH);
    }

    private void uploadNext(int remaining) {
        JsonObject batch = remaining <= 0 ? null : queue.pollFirst();
        if (batch == null) {
            uploading = false;
            return;
        }
        client.post("/v1/observations", batch).whenComplete((result, error) -> context.scheduler().post(() -> {
            if (error != null) {
                queue.addFirst(batch);
                uploading = false;
                return;
            }
            uploadNext(remaining - 1);
        }));
    }

    private void fetchPrices() {
        if (fetching || !client.available() || Minecraft.getInstance().player == null) {
            return;
        }
        List<String> keys = context.market().drainWanted(MAX_KEYS_PER_REQUEST);
        if (keys.isEmpty()) {
            return;
        }
        JsonObject body = new JsonObject();
        JsonArray array = new JsonArray();
        keys.forEach(array::add);
        body.add("keys", array);
        fetching = true;
        client.post("/v1/prices/batch", body).whenComplete((result, error) -> context.scheduler().post(() -> {
            fetching = false;
            if (error != null) {
                return;
            }
            applyPrices(result.getAsJsonObject("prices"));
        }));
    }

    private void applyPrices(JsonObject prices) {
        for (Map.Entry<String, JsonElement> entry : prices.entrySet()) {
            if (entry.getValue().isJsonNull()) {
                continue;
            }
            JsonObject p = entry.getValue().getAsJsonObject();
            context.market().backend().put(entry.getKey(), new PriceStats(p.get("lowestUnitCents").getAsLong(),
                    p.get("medianUnitCents").getAsLong(), p.get("listings").getAsInt(), p.get("units").getAsInt(),
                    p.get("lastSeen").getAsLong()));
        }
        context.market().backendUpdated();
    }
}
