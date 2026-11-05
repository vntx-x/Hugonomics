package dev.hugomarket.module.trades;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.mixin.AbstractContainerScreenAccessor;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.parse.ChatPatterns;
import dev.hugomarket.store.AhTotalsTracker;
import dev.hugomarket.store.SellStats;
import dev.hugomarket.store.TradeLog;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public final class TradeLogModule implements Module {
    private static final long BUY_CONFIRM_WINDOW_MS = 15_000;

    private ModuleContext context;
    private AhListingParser parser;
    private TradeLog log;
    private AhTotalsTracker totals;
    private List<Listing> currentPage = List.of();
    private Listing clicked;
    private long clickedAt;
    private boolean enabled;

    @Override
    public String id() {
        return "trades";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        parser = new AhListingParser(context.guiRules());
        log = new TradeLog(context.dataDir().resolve("trades.jsonl"));
        log.load(System.currentTimeMillis());
        totals = new AhTotalsTracker(context.dataDir().resolve("ah-totals.json"));
        totals.load();
        enabled = true;
        context.screens().onUpdate(this::onScreenUpdate);
        context.screens().onClose(() -> currentPage = List.of());
        context.overlays().addClickHook(this::onClick);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onGameMessage(message.getString()));
        context.menu().add("hugomarket.profit.open", this::openProfit);
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    public void attach(SellStats stats) {
        stats.onPayout(p -> log.record(new TradeLog.Trade(p.ts(), TradeLog.Kind.SHOP_SELL, "shop", "Server-Shop", 0, p.totalCents(), "")));
    }

    public TradeLog log() {
        return log;
    }

    private void onScreenUpdate(ScreenUpdate update) {
        if (!enabled || update.kind() != ScreenKind.AH_LIST) {
            currentPage = List.of();
            return;
        }
        AhListingParser.AhPage page = parser.parse(update.view());
        currentPage = page.isMarket() ? page.listings() : List.of();
        if (page.totals() != null) {
            for (TradeLog.Trade trade : totals.observe(page.totals().earnedCents(), page.totals().spentCents(), System.currentTimeMillis())) {
                log.record(trade);
            }
        }
    }

    private void onClick(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        if (!enabled || currentPage.isEmpty()) {
            return;
        }
        Slot slot = ((AbstractContainerScreenAccessor) screen).hugomarket$hoveredSlot();
        if (slot == null) {
            return;
        }
        for (Listing listing : currentPage) {
            if (listing.slot() == slot.index) {
                clicked = listing;
                clickedAt = System.currentTimeMillis();
                return;
            }
        }
    }

    private void onGameMessage(String text) {
        if (!enabled || !ChatPatterns.isAuctionBought(text)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (clicked == null || now - clickedAt > BUY_CONFIRM_WINDOW_MS) {
            return;
        }
        log.record(new TradeLog.Trade(now, TradeLog.Kind.AH_BUY, clicked.key().toString(), clicked.item().displayName(),
                clicked.item().count(), clicked.priceCents(), clicked.seller()));
        totals.noteBuy(clicked.priceCents());
        clicked = null;
    }

    private void openProfit() {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new ProfitScreen(client.screen, log));
    }
}
