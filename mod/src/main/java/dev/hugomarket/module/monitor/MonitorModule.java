package dev.hugomarket.module.monitor;

import dev.hugomarket.HugoMarket;
import dev.hugomarket.config.ModConfig;
import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ScreenKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.ArrayList;
import java.util.List;

public final class MonitorModule implements Module {
    private static final int MIN_INTERVAL_SECONDS = 15;
    private static final int OPEN_TIMEOUT_TICKS = 20 * 10;
    private static final int CLOSE_DELAY_TICKS = 20 * 3;
    private static final int META_RECHECK_TICKS = 20 * 60 * 5;
    private static final int OFFLINE_EXIT_TICKS = 20 * 60;

    private enum Phase {
        IDLE,
        WAITING_FOR_AH,
        VIEWING
    }

    private ModuleContext context;
    private String account = "";
    private boolean enabled;
    private boolean allowedByServer;
    private int intervalTicks;
    private int ticksUntilNext;
    private int phaseTicks;
    private int metaTicks;
    private Phase phase = Phase.IDLE;
    private int pagesReported;
    private int offlineTicks;
    private boolean headless;
    private List<String> queries = List.of("");
    private int queryIndex;

    @Override
    public String id() {
        return "monitor";
    }

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        if (!context.hasBackend()) {
            HugoMarket.LOGGER.warn("monitor needs a backend, staying idle");
            return;
        }
        ModConfig config = context.config().get();
        intervalTicks = Math.max(MIN_INTERVAL_SECONDS, config.monitorIntervalSeconds) * 20;
        ticksUntilNext = intervalTicks;
        account = config.monitorAccount == null ? "" : config.monitorAccount.trim();
        if (account.isEmpty()) {
            HugoMarket.LOGGER.warn("monitor needs monitorAccount in the config, staying idle");
            return;
        }
        if (!account.equalsIgnoreCase(Minecraft.getInstance().getUser().getName())) {
            HugoMarket.LOGGER.info("monitor idle, logged in account is not {}", account);
            return;
        }
        headless = config.monitorHeadless;
        queries = new ArrayList<>();
        queries.add("");
        for (String query : config.monitorQueries) {
            if (query != null && !query.isBlank() && !queries.contains(query.trim())) {
                queries.add(query.trim());
            }
        }
        enabled = true;
        context.screens().onUpdate(this::onScreenUpdate);
        context.scheduler().onTick(this::tick);
        checkMeta();
        HugoMarket.LOGGER.info("monitor enabled, interval {} s", intervalTicks / 20);
    }

    @Override
    public void onDisable() {
        enabled = false;
        phase = Phase.IDLE;
    }

    private void checkMeta() {
        context.backend().get("/v1/meta").whenComplete((meta, error) -> context.scheduler().post(() -> {
            if (error != null) {
                allowedByServer = false;
                return;
            }
            allowedByServer = meta.has("flags") && meta.getAsJsonObject("flags").has("monitor")
                    && meta.getAsJsonObject("flags").get("monitor").getAsBoolean();
            if (!allowedByServer) {
                HugoMarket.LOGGER.info("monitor not released by the server flag, idle");
            }
        }));
    }

    private void tick(Minecraft client) {
        if (!enabled) {
            return;
        }
        if (++metaTicks >= META_RECHECK_TICKS) {
            metaTicks = 0;
            checkMeta();
        }
        if (client.player == null || !context.server().active()) {
            phase = Phase.IDLE;
            if (headless && ++offlineTicks >= OFFLINE_EXIT_TICKS) {
                HugoMarket.LOGGER.warn("monitor offline for 60 s, exiting so the supervisor restarts the client");
                client.stop();
            }
            return;
        }
        offlineTicks = 0;
        if (!allowedByServer) {
            phase = Phase.IDLE;
            return;
        }
        switch (phase) {
            case IDLE -> {
                if (--ticksUntilNext > 0 || client.screen != null) {
                    return;
                }
                String query = queries.get(queryIndex);
                queryIndex = (queryIndex + 1) % queries.size();
                client.player.connection.sendCommand(query.isEmpty() ? "ah" : "ah " + query);
                phase = Phase.WAITING_FOR_AH;
                phaseTicks = 0;
            }
            case WAITING_FOR_AH -> {
                if (++phaseTicks > OPEN_TIMEOUT_TICKS) {
                    HugoMarket.LOGGER.warn("monitor: AH did not open within 10 s (screen {})", client.screen == null ? "none" : client.screen.getClass().getSimpleName());
                    reset(client);
                }
            }
            case VIEWING -> {
                if (++phaseTicks >= CLOSE_DELAY_TICKS) {
                    reset(client);
                }
            }
        }
    }

    private void onScreenUpdate(ScreenUpdate update) {
        if (phase == Phase.WAITING_FOR_AH && update.kind() == ScreenKind.AH_LIST) {
            phase = Phase.VIEWING;
            phaseTicks = 0;
            pagesReported++;
            if (pagesReported % 10 == 1) {
                HugoMarket.LOGGER.info("monitor saw AH page {} (query index {})", pagesReported, queryIndex);
            }
        }
    }

    private void reset(Minecraft client) {
        if (client.screen instanceof AbstractContainerScreen<?> && client.player != null) {
            client.player.closeContainer();
        }
        phase = Phase.IDLE;
        ticksUntilNext = intervalTicks;
    }

    public int pagesReported() {
        return pagesReported;
    }
}
