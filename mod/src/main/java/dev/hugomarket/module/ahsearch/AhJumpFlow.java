package dev.hugomarket.module.ahsearch;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.HugoControl;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.model.SlotView;
import dev.hugomarket.parse.AhListingParser;
import dev.hugomarket.store.AhFocus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;

final class AhJumpFlow {
    private static final String CHEAPEST_FIRST = "Niedrigster Preis";
    private static final int MAX_SORT_CLICKS = 3;
    private static final int CLICK_DELAY_TICKS = 6;
    private static final int TIMEOUT_TICKS = 20 * 8;

    private final ModuleContext context;
    private AhFocus focus;
    private AbstractContainerScreen<?> screen;
    private int filterSlot = -1;
    private int sortClicks;
    private int clickInTicks = -1;
    private int ageTicks;

    AhJumpFlow(ModuleContext context) {
        this.context = context;
    }

    void start(AhFocus focus) {
        this.focus = focus;
        sortClicks = 0;
        clickInTicks = -1;
        ageTicks = 0;
    }

    void onPage(ScreenUpdate update, AhListingParser.AhPage page) {
        if (focus == null || clickInTicks >= 0) {
            return;
        }
        screen = update.screen();
        if (page.filter() != null && !CHEAPEST_FIRST.equals(page.filter().sort()) && sortClicks < MAX_SORT_CLICKS) {
            filterSlot = filterSlot(update);
            if (filterSlot >= 0) {
                clickInTicks = CLICK_DELAY_TICKS;
                return;
            }
        }
        JumpCheck.Outcome outcome = JumpCheck.check(page.listings(), capacity(update), focus);
        Minecraft client = Minecraft.getInstance();
        switch (outcome) {
            case FOUND -> client.gui.setOverlayMessage(Component.translatable("hugomarket.ah.jump.found"), false);
            case GONE -> {
                context.market().markGone(focus.entryId());
                client.gui.setOverlayMessage(Component.translatable("hugomarket.ah.jump.gone"), false);
            }
            case FURTHER -> client.gui.setOverlayMessage(Component.translatable("hugomarket.ah.jump.further"), false);
        }
        focus = null;
    }

    void tick(Minecraft client) {
        if (focus == null) {
            return;
        }
        if (++ageTicks > TIMEOUT_TICKS) {
            focus = null;
            return;
        }
        if (clickInTicks < 0 || --clickInTicks > 0) {
            return;
        }
        clickInTicks = -1;
        if (client.gameMode == null || client.player == null || screen == null || client.screen != screen) {
            focus = null;
            return;
        }
        client.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, filterSlot, 0, ClickType.PICKUP, client.player);
        sortClicks++;
    }

    private int filterSlot(ScreenUpdate update) {
        for (SlotView slot : update.view().slots()) {
            if (!slot.playerInventory() && !slot.isEmpty() && context.guiRules().controlOf(slot.item()) == HugoControl.FILTER) {
                return slot.index();
            }
        }
        return -1;
    }

    private int capacity(ScreenUpdate update) {
        var rule = context.guiRules().rule(ScreenKind.AH_LIST);
        int count = 0;
        for (SlotView slot : update.view().slots()) {
            if (!slot.playerInventory() && rule.isContentSlot(slot.index())) {
                count++;
            }
        }
        return count;
    }
}
