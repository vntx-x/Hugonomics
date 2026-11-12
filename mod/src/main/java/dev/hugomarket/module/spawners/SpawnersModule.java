package dev.hugomarket.module.spawners;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.module.ahsearch.PriceHistoryCache;
import dev.hugomarket.module.ahsearch.SearchQuery;
import dev.hugomarket.module.watchlist.WatchlistScreen;
import net.minecraft.client.Minecraft;

public final class SpawnersModule implements Module {
    private static final long MAX_STALE_MS = 4_000;

    private ModuleContext context;
    private PriceHistoryCache history;

    @Override
    public String id() {
        return "spawners";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        history = new PriceHistoryCache(context.hasBackend() ? context.backend() : null, context.scheduler());
        context.menu().add("hugomarket.spawners.open", this::open);
    }

    @Override
    public void onDisable() {
    }

    private void open() {
        Runnable refresh = context.hasBackend() ? () -> context.listings().request(MAX_STALE_MS) : () -> { };
        refresh.run();
        WatchlistScreen.Page page = new WatchlistScreen.Page("hugomarket.spawners.title", "hugomarket.spawners.hint", "hugomarket.spawners.empty",
                () -> SpawnerRows.build(context.market().index().query(SearchQuery.EMPTY), context.market()::stats));
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new WatchlistScreen(client.screen, page, context.market(), context.userData(), history, refresh));
    }
}
