package dev.hugomarket.module.sellassist;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.AhView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.model.SlotView;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.parse.HugoGuiRules;
import dev.hugomarket.parse.HugoLore;
import dev.hugomarket.parse.ItemKeyFactory;
import dev.hugomarket.store.Fees;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.OverlayProvider;
import dev.hugomarket.ui.PanelPlacement;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;

import java.util.List;

public final class SellAssistModule implements Module, OverlayProvider {
    private static final int PANEL_WIDTH = 190;

    private ModuleContext context;
    private Fees fees;
    private AhListingParser listingParser;
    private PendingSale pending;

    @Override
    public String id() {
        return "sellassist";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        fees = Fees.load();
        listingParser = new AhListingParser(context.guiRules());
        context.screens().onUpdate(this::onScreenUpdate);
        context.overlays().addProvider(this);
    }

    @Override
    public void onDisable() {
        context.overlays().removeProvider(this);
        pending = null;
    }

    @Override
    public List<OverlayPanel> createPanels(AbstractContainerScreen<?> screen) {
        if (pending == null || !(screen instanceof AnvilScreen anvil) || !isPriceInput(screen)) {
            return List.of();
        }
        SellAssistPanel panel = new SellAssistPanel(anvil, pending, context.market(), fees, context.userData());
        PanelPlacement.rightOf(screen, panel, PANEL_WIDTH);
        return List.of(panel);
    }

    private boolean isPriceInput(AbstractContainerScreen<?> screen) {
        HugoGuiRules.ScreenRule rule = context.guiRules().rule(ScreenKind.PRICE_INPUT);
        return rule != null && rule.title().equals(screen.getTitle().getString());
    }

    private void onScreenUpdate(ScreenUpdate update) {
        switch (update.kind()) {
            case AH_SELL_CONFIRM -> capture(update);
            case AH_CREATE_CONFIRM -> rememberPrice(update);
            case AH_LIST -> {
                pending = null;
                rememberOwnListings(update);
            }
            case PRICE_INPUT -> {
            }
            default -> pending = null;
        }
    }

    private void rememberPrice(ScreenUpdate update) {
        if (pending == null) {
            return;
        }
        HugoGuiRules.ScreenRule rule = context.guiRules().rule(ScreenKind.AH_CREATE_CONFIRM);
        for (SlotView slot : update.view().slots()) {
            if (slot.playerInventory() || slot.isEmpty() || !rule.isContentSlot(slot.index())) {
                continue;
            }
            HugoLore.price(slot.item().lore()).ifPresent(cents ->
                    context.userData().rememberUnitPrice(pending.key().toString(), Math.round((double) cents / pending.count())));
        }
    }

    private void rememberOwnListings(ScreenUpdate update) {
        AhListingParser.AhPage page = listingParser.parse(update.view());
        if (page.view() != AhView.OWN_ACTIVE && page.view() != AhView.OWN_SOLD) {
            return;
        }
        for (Listing listing : page.listings()) {
            if (listing.own()) {
                context.userData().rememberUnitPrice(listing.key().toString(), listing.unitPriceCents());
            }
        }
    }

    private void capture(ScreenUpdate update) {
        HugoGuiRules.ScreenRule rule = context.guiRules().rule(ScreenKind.AH_SELL_CONFIRM);
        for (SlotView slot : update.view().slots()) {
            if (!slot.playerInventory() && !slot.isEmpty() && rule.isContentSlot(slot.index())
                    && context.guiRules().controlOf(slot.item()) == null) {
                pending = new PendingSale(slot.item(), ItemKeyFactory.of(slot.item()));
                return;
            }
        }
    }
}
