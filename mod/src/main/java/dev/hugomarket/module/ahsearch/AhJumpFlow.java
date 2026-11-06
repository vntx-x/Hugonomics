package dev.hugomarket.module.ahsearch;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.HugoControl;
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
    private static final int MAX_PAGES = 15;
    // Hugo fills the page over several slot packets; wait until it stops changing before judging it.
    private static final int SETTLE_TICKS = 4;
    private static final int TIMEOUT_TICKS = 20 * 20;
    // Clicking "next" on the last page sends nothing back, so silence after a page click means the end.
    private static final int NO_ANSWER_TICKS = 30;

    private final ModuleContext context;
    private AhFocus focus;
    private AbstractContainerScreen<?> screen;
    private AhListingParser.AhPage page;
    private int filterSlot = -1;
    private int nextSlot = -1;
    private int sortClicks;
    private int pages;
    private int settleTicks = -1;
    private int awaitingTicks = -1;
    private boolean pagedLast;
    private int ageTicks;

    AhJumpFlow(ModuleContext context) {
        this.context = context;
    }

    void start(AhFocus focus) {
        this.focus = focus;
        page = null;
        sortClicks = 0;
        pages = 0;
        settleTicks = -1;
        awaitingTicks = -1;
        pagedLast = false;
        ageTicks = 0;
    }

    void onPage(ScreenUpdate update, AhListingParser.AhPage page) {
        if (focus == null) {
            return;
        }
        this.screen = update.screen();
        this.page = page;
        filterSlot = controlSlot(update.view(), HugoControl.FILTER);
        nextSlot = controlSlot(update.view(), HugoControl.NEXT_PAGE);
        settleTicks = SETTLE_TICKS;
    }

    void tick(Minecraft client) {
        if (focus == null) {
            return;
        }
        if (++ageTicks > TIMEOUT_TICKS) {
            finish(client, "hugomarket.ah.jump.further");
            return;
        }
        if (awaitingTicks >= 0 && settleTicks < 0 && ++awaitingTicks > NO_ANSWER_TICKS) {
            awaitingTicks = -1;
            if (pagedLast) {
                context.market().markGone(focus.entryId());
                finish(client, "hugomarket.ah.jump.gone");
            } else {
                finish(client, "hugomarket.ah.jump.further");
            }
            return;
        }
        if (settleTicks < 0 || --settleTicks > 0) {
            return;
        }
        awaitingTicks = -1;
        settleTicks = -1;
        if (client.gameMode == null || client.player == null || screen == null || client.screen != screen || page == null) {
            focus = null;
            return;
        }
        if (page.filter() != null && !CHEAPEST_FIRST.equals(page.filter().sort()) && sortClicks < MAX_SORT_CLICKS && filterSlot >= 0) {
            click(client, filterSlot);
            pagedLast = false;
            sortClicks++;
            return;
        }
        switch (JumpCheck.check(page.listings(), nextSlot >= 0, focus)) {
            case FOUND -> finish(client, "hugomarket.ah.jump.found");
            case GONE -> {
                context.market().markGone(focus.entryId());
                finish(client, "hugomarket.ah.jump.gone");
            }
            case NEXT_PAGE -> {
                if (++pages > MAX_PAGES) {
                    finish(client, "hugomarket.ah.jump.further");
                } else {
                    click(client, nextSlot);
                    pagedLast = true;
                }
            }
        }
    }

    private void finish(Minecraft client, String message) {
        client.gui.setOverlayMessage(Component.translatable(message), false);
        focus = null;
    }

    private void click(Minecraft client, int slot) {
        awaitingTicks = 0;
        client.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slot, 0, ClickType.PICKUP, client.player);
    }

    private int controlSlot(ContainerView view, HugoControl control) {
        for (SlotView slot : view.slots()) {
            if (!slot.playerInventory() && !slot.isEmpty() && context.guiRules().controlOf(slot.item()) == control) {
                return slot.index();
            }
        }
        return -1;
    }
}
