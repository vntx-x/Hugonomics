package dev.hugomarket.module.alerts;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.Money;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.module.ahsearch.ListingIndex;
import dev.hugomarket.module.ahsearch.SearchQuery;
import dev.hugomarket.parse.AhListingParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public final class AlertsModule implements Module {
    private ModuleContext context;
    private AhListingParser parser;
    private static final int REMOTE_POLL_TICKS = 20 * 120;

    private final AlertRules rules = new AlertRules();
    private boolean enabled;
    private int ticks;
    private long lastCheckedFetch;

    @Override
    public String id() {
        return "alerts";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        parser = new AhListingParser(context.guiRules());
        enabled = true;
        context.screens().onUpdate(this::onScreenUpdate);
        if (context.hasBackend()) {
            context.scheduler().onTick(this::tick);
            context.market().onChange(this::checkRemote);
        }
    }

    private void tick(Minecraft client) {
        if (!enabled || ++ticks % REMOTE_POLL_TICKS != 0 || client.player == null || !context.server().active()
                || context.userData().alerts().isEmpty()) {
            return;
        }
        context.listings().request(REMOTE_POLL_TICKS * 50L - 5_000);
    }

    private void checkRemote() {
        if (!enabled || context.userData().alerts().isEmpty() || context.listings().lastFetchedAt() == lastCheckedFetch) {
            return;
        }
        lastCheckedFetch = context.listings().lastFetchedAt();
        List<Listing> listings = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (ListingIndex.Entry entry : context.market().index().query(SearchQuery.EMPTY)) {
            if (ListingIndex.isRemote(entry)) {
                listings.add(entry.listing());
                ids.add(entry.id());
            }
        }
        for (AlertRules.Hit hit : rules.check(listings, ids, context.userData().alerts(), System.currentTimeMillis())) {
            notify(hit);
        }
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    private void onScreenUpdate(ScreenUpdate update) {
        if (!enabled || update.kind() != ScreenKind.AH_LIST || context.userData().alerts().isEmpty()) {
            return;
        }
        AhListingParser.AhPage page = parser.parse(update.view());
        if (!page.isMarket()) {
            return;
        }
        List<Listing> listings = page.listings();
        List<String> ids = new ArrayList<>(listings.size());
        long now = update.snapshot().capturedAt();
        for (Listing listing : listings) {
            long expiryBucket = (now + listing.expiresInSeconds() * 1000) / 10_000;
            ids.add(listing.seller() + "|" + listing.key() + "|" + listing.priceCents() + "|" + listing.item().count() + "|" + expiryBucket);
        }
        for (AlertRules.Hit hit : rules.check(listings, ids, context.userData().alerts(), now)) {
            notify(hit);
        }
    }

    private void notify(AlertRules.Hit hit) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        Listing listing = hit.listing();
        String seller = listing.seller().isEmpty() ? I18n.get("hugomarket.alert.server") : listing.seller();
        Component message = Component.literal("[Hugonomics] ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable("hugomarket.alert.hit", listing.item().displayName(), listing.item().count(),
                        Money.format(listing.unitPriceCents()), Money.format(hit.alertUnitCents()), seller)
                        .withStyle(ChatFormatting.WHITE));
        client.gui.getChat().addMessage(message);
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.4f));
    }
}
