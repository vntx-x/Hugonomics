package dev.hugomarket.module.watchalerts;

import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.Money;
import dev.hugomarket.module.ahsearch.Deals;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.module.ahsearch.SearchQuery;
import dev.hugomarket.ui.ItemLabel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public final class WatchAlertsModule implements Module {
    private static final int POLL_TICKS = 20 * 30;

    private ModuleContext context;
    private WatchBargains bargains;
    private boolean enabled;
    private int ticks;
    private long lastCheckedFetch;

    @Override
    public String id() {
        return "watchalerts";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        bargains = new WatchBargains();
        enabled = true;
        if (!context.hasBackend()) {
            return;
        }
        context.scheduler().onTick(this::tick);
        context.market().onChange(this::check);
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    private void tick(Minecraft client) {
        if (!enabled || ++ticks % POLL_TICKS != 0 || client.player == null || !context.server().active()
                || context.userData().watchlist().isEmpty()) {
            return;
        }
        context.listings().request(POLL_TICKS * 50L - 5_000);
    }

    private void check() {
        if (!enabled || context.userData().watchlist().isEmpty() || context.listings().lastFetchedAt() == lastCheckedFetch) {
            return;
        }
        lastCheckedFetch = context.listings().lastFetchedAt();
        List<ListingIndex.Entry> remote = new ArrayList<>();
        for (ListingIndex.Entry entry : context.market().index().query(SearchQuery.EMPTY)) {
            if (ListingIndex.isRemote(entry)) {
                remote.add(entry);
            }
        }
        List<WatchBargains.Hit> hits = bargains.check(remote, context.userData().watchlist(), context.market()::stats);
        if (context.server().active()) {
            hits.forEach(this::notify);
        }
    }

    private void notify(WatchBargains.Hit hit) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        Listing listing = hit.entry().listing();
        Component message = Component.literal("[Hugonomics] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable("hugomarket.watchalerts.hit", listing.item().count(),
                        ItemLabel.of(listing.key(), listing.item().displayName()), Money.format(listing.unitPriceCents()),
                        Deals.deltaText(hit.ratio())).withStyle(ChatFormatting.WHITE));
        client.gui.getChat().addMessage(message);
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f));
    }
}
