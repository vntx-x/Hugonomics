package dev.hugomarket.module.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hugomarket.HugoMarket;
import dev.hugomarket.config.ModConfig;
import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.net.BackendClient;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.store.PriceStats;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
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
    private static final long MIN_RESEND_GAP_MS = 60_000;
    private static final int LISTINGS_INTERVAL_TICKS = 20 * 30;
    private static final long LISTINGS_MIN_GAP_MS = 25_000;
    private static final long LISTINGS_MAX_AGE_MS = 20 * 60_000;

    private ModuleContext context;
    private AhListingParser parser;
    private BackendClient client;
    private String clientId;
    private String pepper;
    private boolean uploadsAllowed;
    private boolean metaLoaded;
    private boolean enabled;
    private final Deque<JsonObject> queue = new ArrayDeque<>();
    private final UploadThrottle throttle = new UploadThrottle(MIN_RESEND_GAP_MS);
    private int ticks;
    private boolean uploading;
    private boolean fetching;
    private boolean fetchingListings;
    private long lastListingsFetch;

    @Override
    public String id() {
        return "sync";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        if (!context.hasBackend()) {
            HugoMarket.LOGGER.info("sync enabled but backendUrl is empty, staying idle");
            return;
        }
        ModConfig config = context.config().get();
        if (config.clientId == null || config.clientId.isBlank()) {
            config.clientId = UUID.randomUUID().toString();
            context.config().save();
        }
        clientId = clientIdFor(config, Minecraft.getInstance().getUser().getName());
        parser = new AhListingParser(context.guiRules());
        client = context.backend();
        enabled = true;
        context.screens().onUpdate(this::onScreenUpdate);
        context.scheduler().onTick(mc -> tick());
        loadMeta();
    }

    @Override
    public void onDisable() {
        enabled = false;
        queue.clear();
        context.market().backend().saveIfDirty();
    }

    static String clientIdFor(ModConfig config, String account) {
        String monitor = config.monitorAccount == null ? "" : config.monitorAccount.trim();
        if (monitor.isEmpty() || !monitor.equalsIgnoreCase(account)) {
            return config.clientId;
        }
        return UUID.nameUUIDFromBytes((config.clientId + "|" + monitor.toLowerCase(java.util.Locale.ROOT)).getBytes(StandardCharsets.UTF_8)).toString();
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
        if (!enabled || update.kind() != ScreenKind.AH_LIST) {
            return;
        }
        fetchListings();
        if (!metaLoaded || !uploadsAllowed) {
            return;
        }
        AhListingParser.AhPage page = parser.parse(update.view());
        if (!page.isMarket() || page.listings().isEmpty()) {
            return;
        }
        long capturedAt = update.snapshot().capturedAt();
        List<Listing> due = throttle.filter(page.listings(), capturedAt);
        if (due.isEmpty()) {
            return;
        }
        JsonObject batch = ObservationBatch.build(due, clientId, HugoMarket.modVersion(),
                HugoMarket.minecraftVersion(), capturedAt, pepper);
        if (batch.getAsJsonArray("listings").isEmpty()) {
            return;
        }
        if (queue.size() >= MAX_QUEUE) {
            queue.pollFirst();
        }
        queue.addLast(batch);
    }

    private void tick() {
        if (!enabled) {
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
        if (ticks % LISTINGS_INTERVAL_TICKS == 0) {
            ScreenUpdate current = context.screens().current();
            if (current != null && current.kind() == ScreenKind.AH_LIST) {
                fetchListings();
            }
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

    private void fetchListings() {
        long now = System.currentTimeMillis();
        if (fetchingListings || now - lastListingsFetch < LISTINGS_MIN_GAP_MS || !client.available()) {
            return;
        }
        fetchingListings = true;
        lastListingsFetch = now;
        client.get("/v1/listings?maxAge=" + LISTINGS_MAX_AGE_MS).whenComplete((result, error) -> context.scheduler().post(() -> {
            fetchingListings = false;
            if (error != null) {
                return;
            }
            long received = System.currentTimeMillis();
            context.market().replaceRemoteListings(ListingsJson.parse(result, received), received);
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
            context.market().backend().put(entry.getKey(), PriceJson.stats(entry.getValue().getAsJsonObject()));
        }
        context.market().backendUpdated();
    }
}
