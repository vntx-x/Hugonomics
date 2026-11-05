package dev.hugomarket.module.networth;

import dev.hugomarket.container.ItemViews;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Money;
import dev.hugomarket.store.ContainerValuation;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Label;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
*///?}

final class NetWorthScreen extends Screen {
    private static final int WIDTH = 300;

    private final Screen parent;
    private final NetWorthPanel panel;

    NetWorthScreen(Screen parent, NetWorthStore store, MarketSession market) {
        super(Component.translatable("hugomarket.networth.title"));
        this.parent = parent;
        this.panel = new NetWorthPanel(store, market);
    }

    @Override
    protected void init() {
        panel.setBounds((width - WIDTH) / 2, (height - NetWorthPanel.HEIGHT) / 2, WIDTH, NetWorthPanel.HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        panel.render(new Gfx(graphics), mouseX, mouseY);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //? if >=1.21.9 {
    /*@Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
    *///?} else {
    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scancode, modifiers);
    }
    //?}

    private static final class NetWorthPanel extends OverlayPanel {
        static final int HEIGHT = Theme.PADDING * 2 + headerHeight() + Theme.LINE * 7;

        private final List<Label> lines = new ArrayList<>();

        NetWorthPanel(NetWorthStore store, MarketSession market) {
            setTitle(I18n.get("hugomarket.networth.title"));
            long now = System.currentTimeMillis();
            long inventory = inventoryValue(market);
            long total = inventory + store.balance().cents() + store.enderchest().cents() + store.auctions().cents();
            String balance = store.balance().known()
                    ? I18n.get("hugomarket.networth.line", Money.format(store.balance().cents()), age(now, store.balance().at()))
                    : I18n.get("hugomarket.networth.unknown");
            lines.add(add(new Label(() -> I18n.get("hugomarket.networth.balance", balance), Theme.TEXT)));
            lines.add(add(new Label(() -> I18n.get("hugomarket.networth.inventory", Money.format(inventory)), Theme.TEXT)));
            lines.add(add(new Label(() -> I18n.get("hugomarket.networth.enderchest", part(store.enderchest(), now)), Theme.TEXT)));
            lines.add(add(new Label(() -> I18n.get("hugomarket.networth.auctions", part(store.auctions(), now)), Theme.TEXT)));
            lines.add(add(new Label(() -> I18n.get("hugomarket.networth.total", Money.format(total)), Theme.GOLD)));
            lines.add(add(new Label(() -> I18n.get("hugomarket.networth.hint_values"))));
            lines.add(add(new Label(() -> I18n.get(store.balance().known() ? "hugomarket.networth.hint_sources" : "hugomarket.networth.hint_balance"))));
        }

        private static String part(NetWorthStore.Part part, long now) {
            return part.known() ? I18n.get("hugomarket.networth.line", Money.format(part.cents()), age(now, part.at()))
                    : I18n.get("hugomarket.networth.unknown");
        }

        private static String age(long now, long at) {
            long seconds = Math.max(0, (now - at) / 1000);
            if (seconds < 90) {
                return I18n.get("hugomarket.networth.age_s", seconds);
            }
            if (seconds < 5400) {
                return I18n.get("hugomarket.networth.age_m", seconds / 60);
            }
            if (seconds < 172_800) {
                return I18n.get("hugomarket.networth.age_h", seconds / 3600);
            }
            return I18n.get("hugomarket.networth.age_d", seconds / 86_400);
        }

        private static long inventoryValue(MarketSession market) {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) {
                return 0;
            }
            Inventory inventory = client.player.getInventory();
            List<ItemView> items = new ArrayList<>();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    items.add(ItemViews.of(stack));
                }
            }
            return ContainerValuation.ofItems(items, market).ahLowestCents();
        }

        @Override
        protected void layout() {
            int cursorY = contentTop();
            for (Label label : lines) {
                label.setBounds(x + Theme.PADDING, cursorY, width - Theme.PADDING * 2, Theme.LINE);
                cursorY += Theme.LINE;
            }
        }
    }
}
