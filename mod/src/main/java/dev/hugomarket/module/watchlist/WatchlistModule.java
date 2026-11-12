package dev.hugomarket.module.watchlist;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.module.ahsearch.PriceHistoryCache;
import dev.hugomarket.module.ahsearch.SearchQuery;
import net.minecraft.client.Minecraft;

public final class WatchlistModule implements Module {
    private static final long MAX_STALE_MS = 4_000;

    private ModuleContext context;
    private PriceHistoryCache history;

    @Override
    public String id() {
        return "watchlist";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        history = new PriceHistoryCache(context.hasBackend() ? context.backend() : null, context.scheduler());
        context.menu().add("hugomarket.watchlist.open", this::open);
    }

    @Override
    public void onDisable() {
    }

    private void open() {
        Runnable refresh = context.hasBackend() ? () -> context.listings().request(MAX_STALE_MS) : () -> { };
        refresh.run();
        Minecraft client = Minecraft.getInstance();
        WatchlistScreen.Page page = new WatchlistScreen.Page("hugomarket.watchlist.title", "hugomarket.watchlist.hint", "hugomarket.watchlist.empty",
                () -> WatchlistRows.build(context.userData().watchlist(), context.market().index().query(SearchQuery.EMPTY), context.market()::stats));
        client.setScreen(new WatchlistScreen(client.screen, page, context.market(), context.userData(), history, refresh));
    }
}
