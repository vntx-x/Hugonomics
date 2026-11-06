package dev.hugomarket.module.monitor;

import dev.hugomarket.HugoMarket;
import dev.hugomarket.config.ModConfig;
import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.HugoControl;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.model.SlotView;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.store.TextLog;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;
import java.util.concurrent.ThreadLocalRandom;

public final class MonitorModule implements Module {
    private static final int MIN_INTERVAL_SECONDS = 3;
    private static final int MIN_REFRESH_SECONDS = 3;
    private static final int OPEN_TIMEOUT_TICKS = 20 * 10;
    private static final int CLOSE_DELAY_TICKS = 20 * 3;
    private static final int UNCHANGED_REFRESH_TICKS = 20 * 2;
    private static final int META_RECHECK_TICKS = 20 * 60 * 5;
    private static final int OFFLINE_EXIT_TICKS = 20 * 60;
    private static final int STATS_EVERY_REFRESHES = 60;
    private static final int SEEN_MEMORY = 500;

    private static final String NEWEST_FIRST = "Neueste Auktionen";
    private static final String CHEAPEST_FIRST = "Niedrigster Preis";
    private static final int MAX_SORT_CLICKS = 3;
    private static final int SORT_CLICK_DELAY_TICKS = 10;

    private enum Phase {
        IDLE,
        WAITING_FOR_AH,
        SORTING,
        LIVE,
        SWEEP_VIEWING
    }

    private ModuleContext context;
    private String account = "";
    private boolean enabled;
    private boolean allowedByServer;
    private int intervalTicks;
    private int refreshTicks;
    private int refreshMaxTicks;
    private int nextRefreshTicks;
    private int sweepEvery;
    private int ticksUntilNext;
    private int phaseTicks;
    private int metaTicks;
    private Phase phase = Phase.IDLE;
    private boolean sweeping;
    private int pagesReported;
    private int offlineTicks;
    private boolean headless;
    private boolean sneak;
    private List<String> queries = List.of();
    private int queryIndex;
    private TextLog log;
    private boolean blockedLogged;
    private AhListingParser parser;
    private AbstractContainerScreen<?> ahScreen;
    private int filterSlot = -1;
    private int refreshSlot = -1;
    private int sortClicks;
    private boolean refreshPending;
    private int refreshesSinceSweep;
    private int refreshes;
    private int newSinceStats;
    private int fullPagesSinceStats;
    private final SequencedSet<String> seen = new LinkedHashSet<>();

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
        refreshTicks = Math.max(MIN_REFRESH_SECONDS, config.monitorRefreshSeconds) * 20;
        refreshMaxTicks = Math.max(refreshTicks / 20, config.monitorRefreshMaxSeconds) * 20;
        nextRefreshTicks = refreshTicks;
        sweepEvery = Math.max(0, config.monitorSweepEvery);
        ticksUntilNext = 20 * 5;
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
        sneak = config.monitorSneak;
        queries = new ArrayList<>();
        for (String query : config.monitorQueries) {
            if (query != null && !query.isBlank() && !queries.contains(query.trim())) {
                queries.add(query.trim());
            }
        }
        log = new TextLog(context.dataDir().resolve("monitor.log"));
        parser = new AhListingParser(context.guiRules());
        enabled = true;
        context.screens().onUpdate(this::onScreenUpdate);
        context.scheduler().onTick(this::tick);
        checkMeta();
        HugoMarket.LOGGER.info("monitor enabled, refresh {}-{} s, sweep every {}", refreshTicks / 20, refreshMaxTicks / 20, sweepEvery);
        log.line("enabled as " + account + ", refresh " + refreshTicks / 20 + "-" + refreshMaxTicks / 20 + " s, sweep every " + sweepEvery
                + " refreshes, queries " + queries);
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
                log.line("meta unavailable: " + error);
                return;
            }
            allowedByServer = meta.has("flags") && meta.getAsJsonObject("flags").has("monitor")
                    && meta.getAsJsonObject("flags").get("monitor").getAsBoolean();
            log.line("meta ok, monitor flag " + allowedByServer);
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
                log.line("offline for 60 s, exiting");
                client.stop();
            }
            return;
        }
        offlineTicks = 0;
        if (sneak && client.screen == null) {
            client.options.keyShift.setDown(true);
        }
        if (!allowedByServer) {
            if (phase != Phase.IDLE) {
                reset(client, intervalTicks);
            }
            return;
        }
        switch (phase) {
            case IDLE -> openAh(client);
            case SORTING -> {
                if (++phaseTicks < SORT_CLICK_DELAY_TICKS) {
                    return;
                }
                if (!ahStillOpen(client) || filterSlot < 0) {
                    reset(client, intervalTicks);
                    return;
                }
                click(client, filterSlot);
                sortClicks++;
                phase = Phase.WAITING_FOR_AH;
                phaseTicks = 0;
            }
            case WAITING_FOR_AH -> {
                if (refreshPending && ++phaseTicks >= UNCHANGED_REFRESH_TICKS && ahStillOpen(client)) {
                    refreshPending = false;
                    phase = Phase.LIVE;
                    phaseTicks = 0;
                    return;
                }
                if (!refreshPending && ++phaseTicks > OPEN_TIMEOUT_TICKS) {
                    String screen = client.screen == null ? "none" : client.screen.getClass().getSimpleName();
                    HugoMarket.LOGGER.warn("monitor: AH did not answer within 10 s (screen {})", screen);
                    log.line("AH did not answer within 10 s, screen " + screen);
                    reset(client, intervalTicks);
                }
            }
            case LIVE -> {
                if (!ahStillOpen(client)) {
                    log.line("AH closed, reopening");
                    reset(client, intervalTicks);
                    return;
                }
                if (++phaseTicks < nextRefreshTicks) {
                    return;
                }
                nextRefreshTicks = ThreadLocalRandom.current().nextInt(refreshTicks, refreshMaxTicks + 1);
                if (sweepEvery > 0 && !queries.isEmpty() && refreshesSinceSweep >= sweepEvery) {
                    refreshesSinceSweep = 0;
                    sweeping = true;
                    reset(client, SORT_CLICK_DELAY_TICKS);
                    return;
                }
                if (refreshSlot < 0) {
                    reset(client, intervalTicks);
                    return;
                }
                click(client, refreshSlot);
                refreshPending = true;
                refreshesSinceSweep++;
                phase = Phase.WAITING_FOR_AH;
                phaseTicks = 0;
            }
            case SWEEP_VIEWING -> {
                if (++phaseTicks >= CLOSE_DELAY_TICKS) {
                    sweeping = false;
                    reset(client, SORT_CLICK_DELAY_TICKS);
                }
            }
        }
    }

    private void openAh(Minecraft client) {
        if (--ticksUntilNext > 0) {
            return;
        }
        if (client.screen instanceof PauseScreen) {
            client.setScreen(null);
        }
        if (client.screen != null) {
            if (!blockedLogged) {
                blockedLogged = true;
                log.line("waiting, screen open: " + client.screen.getClass().getSimpleName());
            }
            return;
        }
        blockedLogged = false;
        String command = "ah";
        if (sweeping) {
            command = "ah " + queries.get(queryIndex);
            queryIndex = (queryIndex + 1) % queries.size();
        }
        client.player.connection.sendCommand(command);
        log.line("sent /" + command);
        sortClicks = 0;
        phase = Phase.WAITING_FOR_AH;
        phaseTicks = 0;
    }

    private void onScreenUpdate(ScreenUpdate update) {
        if (phase != Phase.WAITING_FOR_AH || update.kind() != ScreenKind.AH_LIST) {
            return;
        }
        refreshPending = false;
        ahScreen = update.screen();
        filterSlot = controlSlot(update.view(), HugoControl.FILTER);
        refreshSlot = controlSlot(update.view(), HugoControl.REFRESH);
        phaseTicks = 0;
        AhListingParser.AhPage page = parser.parse(update.view());
        String wanted = sweeping ? CHEAPEST_FIRST : NEWEST_FIRST;
        if (page.filter() != null && !wanted.equals(page.filter().sort()) && sortClicks < MAX_SORT_CLICKS && filterSlot >= 0) {
            log.line("sort is '" + page.filter().sort() + "', want '" + wanted + "', clicking filter (" + (sortClicks + 1) + ")");
            phase = Phase.SORTING;
            return;
        }
        pagesReported++;
        if (sweeping) {
            log.line("sweep page seen (" + page.listings().size() + " listings)");
            phase = Phase.SWEEP_VIEWING;
            return;
        }
        countNew(page.listings());
        phase = Phase.LIVE;
    }

    private void countNew(List<Listing> listings) {
        int fresh = 0;
        for (Listing listing : listings) {
            String identity = listing.seller() + "|" + listing.key() + "|" + listing.item().count() + "|" + listing.priceCents();
            if (seen.add(identity)) {
                fresh++;
                if (seen.size() > SEEN_MEMORY) {
                    seen.removeFirst();
                }
            }
        }
        boolean firstPage = refreshes == 0;
        refreshes++;
        if (!firstPage) {
            newSinceStats += fresh;
            if (!listings.isEmpty() && fresh == listings.size()) {
                fullPagesSinceStats++;
            }
        }
        if (refreshes % STATS_EVERY_REFRESHES == 0) {
            log.line("live: " + newSinceStats + " new listings in the last " + STATS_EVERY_REFRESHES + " refreshes, "
                    + fullPagesSinceStats + " pages were completely new (refresh faster if this is above 0)");
            newSinceStats = 0;
            fullPagesSinceStats = 0;
        }
    }

    private boolean ahStillOpen(Minecraft client) {
        return client.gameMode != null && ahScreen != null && client.screen == ahScreen;
    }

    private void click(Minecraft client, int slot) {
        client.gameMode.handleInventoryMouseClick(ahScreen.getMenu().containerId, slot, 0, ClickType.PICKUP, client.player);
    }

    private int controlSlot(ContainerView view, HugoControl control) {
        for (SlotView slot : view.slots()) {
            if (!slot.playerInventory() && !slot.isEmpty() && context.guiRules().controlOf(slot.item()) == control) {
                return slot.index();
            }
        }
        return -1;
    }

    private void reset(Minecraft client, int delayTicks) {
        if (client.screen instanceof AbstractContainerScreen<?> && client.player != null) {
            client.player.closeContainer();
        }
        ahScreen = null;
        refreshPending = false;
        phase = Phase.IDLE;
        ticksUntilNext = delayTicks;
    }

    public int pagesReported() {
        return pagesReported;
    }
}
