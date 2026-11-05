package dev.hugomarket.module.orders;

import dev.hugomarket.container.ScreenUpdate;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.Order;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.parse.OrderListParser;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.OverlayProvider;
import dev.hugomarket.ui.PanelPlacement;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OrdersModule implements Module, OverlayProvider {
    private static final int PANEL_WIDTH = 190;

    private ModuleContext context;
    private OrderListParser parser;
    private OrdersPanel panel;
    private AbstractContainerScreen<?> panelScreen;

    @Override
    public String id() {
        return "orders";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        parser = new OrderListParser(context.guiRules());
        context.screens().onUpdate(this::onScreenUpdate);
        context.screens().onClose(() -> panel = null);
        context.overlays().addProvider(this);
    }

    @Override
    public void onDisable() {
        context.overlays().removeProvider(this);
        panel = null;
    }

    @Override
    public List<OverlayPanel> createPanels(AbstractContainerScreen<?> screen) {
        if (!screen.getTitle().getString().equals(context.guiRules().rule(ScreenKind.ORDER_LIST).title())) {
            return List.of();
        }
        panel = new OrdersPanel(screen, context.market());
        panelScreen = screen;
        PanelPlacement.rightOf(screen, panel, PANEL_WIDTH);
        ScreenUpdate current = context.screens().current();
        if (current != null && current.screen() == screen) {
            onScreenUpdate(current);
        }
        return List.of(panel);
    }

    private void onScreenUpdate(ScreenUpdate update) {
        if (update.kind() != ScreenKind.ORDER_LIST) {
            return;
        }
        List<Order> orders = parser.parse(update.view());
        context.market().ordersSeen(orders, update.snapshot().capturedAt());
        if (panel == null || update.screen() != panelScreen) {
            return;
        }
        Map<Integer, ItemStack> icons = new HashMap<>();
        for (Order order : orders) {
            icons.put(order.slot(), update.snapshot().slots().get(order.slot()).stack());
        }
        panel.update(orders, icons);
    }
}
