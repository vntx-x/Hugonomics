package dev.hugomarket.module.hudwatch;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.Money;
import dev.hugomarket.module.ahsearch.Deals;
import dev.hugomarket.module.ahsearch.SearchQuery;
import dev.hugomarket.module.watchlist.WatchlistRows;
import dev.hugomarket.store.HudBoard;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.ui.ItemLabel;
import dev.hugomarket.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class HudWatchModule implements Module {
    private static final int REFRESH_TICKS = 20;
    private static final int POLL_TICKS = 20 * 30;
    private static final int MAX_LINES = 3;
    private static final int MAX_NAME = 22;

    private ModuleContext context;
    private boolean enabled;
    private int ticks;
    private int marketVersion = -1;
    private int watchHash;

    @Override
    public String id() {
        return "hudwatch";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        enabled = true;
        marketVersion = -1;
        context.scheduler().onTick(this::tick);
    }

    @Override
    public void onDisable() {
        enabled = false;
        context.hud().set(HudBoard.WATCHLIST, List.of());
    }

    private void tick(Minecraft client) {
        if (!enabled || ++ticks % REFRESH_TICKS != 0) {
            return;
        }
        Set<String> watchlist = context.userData().watchlist();
        if (context.hasBackend() && ticks % POLL_TICKS == 0 && !watchlist.isEmpty() && context.server().active()) {
            context.listings().request(POLL_TICKS * 50L - 5_000);
        }
        MarketSession market = context.market();
        if (market.version() == marketVersion && watchlist.hashCode() == watchHash) {
            return;
        }
        marketVersion = market.version();
        watchHash = watchlist.hashCode();
        List<HudBoard.Line> lines = new ArrayList<>(MAX_LINES);
        for (WatchlistRows.Row row : WatchlistRows.build(watchlist, market.index().query(SearchQuery.EMPTY), market::stats)) {
            if (!row.hasOffers() || lines.size() >= MAX_LINES) {
                break;
            }
            lines.add(line(row));
        }
        context.hud().set(HudBoard.WATCHLIST, lines);
    }

    private static HudBoard.Line line(WatchlistRows.Row row) {
        var listing = row.cheapest().listing();
        String name = ItemLabel.of(listing.key(), listing.item().displayName());
        if (name.length() > MAX_NAME) {
            name = name.substring(0, MAX_NAME - 1) + "…";
        }
        String price = Money.compact(listing.unitPriceCents());
        if (row.ratio().isEmpty()) {
            return new HudBoard.Line(I18n.get("hugomarket.hudwatch.line", name, price), Theme.TEXT);
        }
        double ratio = row.ratio().getAsDouble();
        String text = I18n.get("hugomarket.hudwatch.line_delta", name, price, Deals.deltaText(ratio));
        return new HudBoard.Line(text, Deals.isBargain(ratio) ? Theme.BARGAIN_FRAME : Theme.TEXT);
    }
}
