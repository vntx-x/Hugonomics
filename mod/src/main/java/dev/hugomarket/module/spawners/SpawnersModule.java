package dev.hugomarket.module.spawners;

import dev.hugomarket.HugoMarket;
import dev.hugomarket.container.ItemViews;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.store.SpawnerPrices;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.OverlayProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SpawnersModule implements Module, OverlayProvider {
    private static final long REFRESH_MS = 30 * 60_000;

    private ModuleContext context;
    private long loadedAt;
    private boolean loading;

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
        context.menu().add("hugomarket.spawners.open", this::open);
        context.overlays().addProvider(this);
        load();
    }

    @Override
    public void onDisable() {
        context.overlays().removeProvider(this);
        context.market().setSpawnerPrices(SpawnerPrices.EMPTY);
        loadedAt = 0;
    }

    @Override
    public List<OverlayPanel> createPanels(AbstractContainerScreen<?> screen) {
        if (!(screen instanceof InventoryScreen) || !context.server().active()) {
            return List.of();
        }
        return List.of(new SpawnerButtonPanel(screen, this::open));
    }

    private void load() {
        if (!context.hasBackend() || loading || System.currentTimeMillis() - loadedAt < REFRESH_MS) {
            return;
        }
        loading = true;
        context.backend().get("/v1/spawners").whenComplete((json, error) -> context.scheduler().post(() -> {
            loading = false;
            if (error != null) {
                HugoMarket.LOGGER.warn("spawner prices unavailable: {}", error.toString());
                return;
            }
            loadedAt = System.currentTimeMillis();
            context.market().setSpawnerPrices(SpawnerPrices.parse(json));
        }));
    }

    private void open() {
        load();
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new SpawnersScreen(client.screen, context.market(), context.hasBackend(), owned(client)));
    }

    private static Map<String, Integer> owned(Minecraft client) {
        if (client.player == null) {
            return Map.of();
        }
        Inventory inventory = client.player.getInventory();
        List<ItemView> items = new ArrayList<>(inventory.getContainerSize());
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                items.add(ItemViews.of(stack));
            }
        }
        return SpawnerHoldings.count(items);
    }
}
