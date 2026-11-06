package dev.hugomarket.module.deals;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.store.Fees;
import net.minecraft.client.Minecraft;

public final class DealsModule implements Module {
    private static final long MAX_STALE_MS = 4_000;

    private ModuleContext context;
    private Fees fees;

    @Override
    public String id() {
        return "deals";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        fees = Fees.load();
        if (!context.hasBackend()) {
            return;
        }
        context.menu().add("hugomarket.deals.open", this::open);
    }

    @Override
    public void onDisable() {
    }

    private void open() {
        context.listings().request(MAX_STALE_MS);
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new DealsScreen(client.screen, context.market(), fees, () -> context.listings().request(MAX_STALE_MS)));
    }
}
