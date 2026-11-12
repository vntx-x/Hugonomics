package dev.hugomarket.module.spawners;

import dev.hugomarket.HugoMarket;
import dev.hugomarket.container.ItemViews;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Money;
import dev.hugomarket.store.SpawnerPrices;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.OverlayProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SpawnersModule implements Module, OverlayProvider {
    private static final long PRICES_REFRESH_MS = 30 * 60_000;
    private static final int DEALS_POLL_TICKS = 20 * 60;
    private static final long LOGIN_RETRY_MS = 10 * 60_000;

    private ModuleContext context;
    private SpawnerExchange exchange;
    private long pricesLoadedAt;
    private boolean pricesLoading;
    private long lastAutoLogin;
    private int ticks;
    private boolean running;

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
        running = true;
        if (context.hasBackend()) {
            exchange = new SpawnerExchange(context.backend(), context.scheduler(), context.config());
            exchange.onEvents(events -> events.forEach(this::notify));
        }
        context.menu().add("hugomarket.spawners.open", this::open);
        context.overlays().addProvider(this);
        context.scheduler().onTick(client -> tick());
        loadPrices();
    }

    @Override
    public void onDisable() {
        running = false;
        context.overlays().removeProvider(this);
        context.market().setSpawnerPrices(SpawnerPrices.EMPTY);
        pricesLoadedAt = 0;
        if (exchange != null) {
            exchange.forget();
        }
    }

    @Override
    public List<OverlayPanel> createPanels(AbstractContainerScreen<?> screen) {
        if (!(screen instanceof InventoryScreen) || !context.server().active()) {
            return List.of();
        }
        return List.of(new SpawnerButtonPanel(screen, this::open));
    }

    private void tick() {
        if (!running || exchange == null || ++ticks % DEALS_POLL_TICKS != 0 || !context.server().active()) {
            return;
        }
        if (exchange.loggedIn()) {
            exchange.refreshDeals(true);
        } else if (exchange.joined() && System.currentTimeMillis() - lastAutoLogin > LOGIN_RETRY_MS) {
            lastAutoLogin = System.currentTimeMillis();
            exchange.login(unused -> { });
        }
    }

    private void loadPrices() {
        if (!context.hasBackend() || pricesLoading || System.currentTimeMillis() - pricesLoadedAt < PRICES_REFRESH_MS) {
            return;
        }
        pricesLoading = true;
        context.backend().get("/v1/spawners").whenComplete((json, error) -> context.scheduler().post(() -> {
            pricesLoading = false;
            if (error != null) {
                HugoMarket.LOGGER.warn("spawner prices unavailable: {}", error.toString());
                return;
            }
            pricesLoadedAt = System.currentTimeMillis();
            context.market().setSpawnerPrices(SpawnerPrices.parse(json));
        }));
    }

    private void open() {
        loadPrices();
        if (exchange != null) {
            exchange.refreshOffers();
            exchange.refreshDeals(false);
        }
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new SpawnersScreen(client.screen, context.market(), exchange, owned(client)));
    }

    private void notify(DealEvents.Event event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        ExchangeDeal deal = event.deal();
        String key = switch (event.kind()) {
            case NEW_REQUEST -> "hugomarket.exchange.notify.new";
            case COUNTER -> "hugomarket.exchange.notify.counter";
            case ACCEPTED -> "hugomarket.exchange.notify.accepted";
            case DECLINED -> "hugomarket.exchange.notify.declined";
        };
        Component message = Component.literal("[Hugonomics] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(key, deal.otherPlayer(), deal.count(), SpawnerNames.of(deal.type()),
                        Money.format(deal.unitCents())).withStyle(ChatFormatting.WHITE));
        client.gui.getChat().addMessage(message);
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f));
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
