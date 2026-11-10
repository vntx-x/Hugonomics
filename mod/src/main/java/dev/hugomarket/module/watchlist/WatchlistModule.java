package dev.hugomarket.module.watchlist;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import net.minecraft.client.Minecraft;

public final class WatchlistModule implements Module {
    private static final long MAX_STALE_MS = 4_000;

    private ModuleContext context;

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
        context.menu().add("hugomarket.watchlist.open", this::open);
    }

    @Override
    public void onDisable() {
    }

    private void open() {
        Runnable refresh = context.hasBackend() ? () -> context.listings().request(MAX_STALE_MS) : () -> { };
        refresh.run();
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new WatchlistScreen(client.screen, context.market(), context.userData(), refresh));
    }
}
