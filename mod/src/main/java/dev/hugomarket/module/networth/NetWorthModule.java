package dev.hugomarket.module.networth;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.AhView;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.module.myauctions.OwnAuctionSummary;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.store.ContainerValuation;
import dev.hugomarket.store.Fees;
import dev.hugomarket.ui.Sidebar;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.OptionalLong;

public final class NetWorthModule implements Module {
    private static final int SCAN_INTERVAL_TICKS = 20;

    private ModuleContext context;
    private NetWorthStore store;
    private AhListingParser parser;
    private Fees fees;
    private boolean enabled;
    private int ticks;
    private List<String> lastSidebar = List.of();
    private long lastSidebarCents = Long.MIN_VALUE;

    @Override
    public String id() {
        return "networth";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        store = new NetWorthStore(context.dataDir().resolve("networth.json"));
        store.load();
        parser = new AhListingParser(context.guiRules());
        fees = Fees.load();
        enabled = true;
        context.scheduler().onTick(this::tick);
        context.screens().onUpdate(this::onScreenUpdate);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onMessage(message.getString()));
        context.menu().add("hugomarket.networth.open", () -> {
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new NetWorthScreen(client.screen, store, context.market()));
        });
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    private void tick(Minecraft client) {
        if (!enabled || ++ticks % SCAN_INTERVAL_TICKS != 0 || !context.server().active()) {
            return;
        }
        List<String> lines = Sidebar.lines(client);
        if (lines.equals(lastSidebar)) {
            return;
        }
        lastSidebar = lines;
        OptionalLong balance = BalanceParser.fromSidebar(lines);
        if (balance.isPresent() && balance.getAsLong() != lastSidebarCents) {
            lastSidebarCents = balance.getAsLong();
            store.setBalance(balance.getAsLong(), System.currentTimeMillis());
        }
    }

    private void onMessage(String text) {
        if (!enabled || !context.server().active()) {
            return;
        }
        OptionalLong balance = BalanceParser.fromMessage(text);
        if (balance.isPresent()) {
            store.setBalance(balance.getAsLong(), System.currentTimeMillis());
        }
    }

    private void onScreenUpdate(ScreenUpdate update) {
        if (!enabled) {
            return;
        }
        if (update.kind() == ScreenKind.ENDERCHEST) {
            store.setEnderchest(ContainerValuation.of(update.view(), context.market()).ahLowestCents(), System.currentTimeMillis());
            return;
        }
        if (update.kind() != ScreenKind.AH_LIST) {
            return;
        }
        AhListingParser.AhPage page = parser.parse(update.view());
        if (page.view() == AhView.OWN_ACTIVE) {
            OwnAuctionSummary summary = OwnAuctionSummary.of(page.view(), page.listings(), context.market(), fees);
            store.setAuctions(summary.netSumCents(), System.currentTimeMillis());
        }
    }
}
