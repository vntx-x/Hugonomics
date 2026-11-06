package dev.hugomarket.module.market;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.ui.KeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class MarketModule implements Module {
    private static final long LISTINGS_MAX_STALE_MS = 4_000;

    private ModuleContext context;
    private MarketData data;
    private KeyMapping key;

    @Override
    public String id() {
        return "market";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        data = new MarketData(context.backend(), context.scheduler(), context.market());
        key = KeyBindings.register("key.hugomarket.market", GLFW.GLFW_KEY_UNKNOWN);
        context.scheduler().onTick(this::tick);
        context.menu().add("hugomarket.market.open", this::open);
    }

    @Override
    public void onDisable() {
    }

    private void tick(Minecraft client) {
        while (key.consumeClick()) {
            if (client.screen == null) {
                open();
            }
        }
    }

    private void open() {
        Minecraft client = Minecraft.getInstance();
        Runnable requestListings = context.listings() == null ? () -> { } : () -> context.listings().request(LISTINGS_MAX_STALE_MS);
        client.setScreen(new MarketScreen(client.screen, new MarketPanel(data, context.market(), context.userData(), requestListings), data));
    }
}
