package dev.hugomarket.module.selllist;

import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Money;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.ItemLabel;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.ListView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

//? if >=1.21.9 {
/*import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

final class SellListScreen extends Screen {
    private static final int WIDTH = 380;
    private static final int MARGIN = 16;

    private final Screen parent;
    private final SellListPanel panel;

    SellListScreen(Screen parent, List<SellPlan.Line> lines, List<ItemView> items, List<ItemStack> stacks) {
        super(Component.translatable("hugomarket.selllist.title"));
        this.parent = parent;
        Map<ItemView, ItemStack> icons = new IdentityHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            icons.put(items.get(i), stacks.get(i));
        }
        this.panel = new SellListPanel(lines, icons);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(WIDTH, width - MARGIN * 2);
        panel.setBounds((width - panelWidth) / 2, MARGIN, panelWidth, height - MARGIN * 2);
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

    private boolean handleKey(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return false;
    }

    //? if >=1.21.9 {
    /*@Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return panel.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return handleKey(event.key()) || super.keyPressed(event);
    }
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        return handleKey(key) || super.keyPressed(key, scancode, modifiers);
    }
    //?}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return panel.mouseScrolled(mouseX, mouseY, vertical) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private static final class SellListPanel extends OverlayPanel {
        private static final int ICON_SIZE = 16;

        private final Map<ItemView, ItemStack> icons;
        private final ListView<SellPlan.Line> list;
        private final String totalText;

        SellListPanel(List<SellPlan.Line> lines, Map<ItemView, ItemStack> icons) {
            this.icons = icons;
            setTitle(I18n.get("hugomarket.selllist.title"));
            totalText = I18n.get("hugomarket.selllist.total", Money.compact(SellPlan.totalNet(lines)));
            setHeaderRight(() -> totalText);
            list = add(new ListView<>(Theme.ROW_HEIGHT, this::renderRow, (line, button) -> copy(line)));
            list.setHoverRenderer((gfx, line, mouseX, mouseY) -> {
                ItemStack stack = icons.get(line.sample());
                if (stack != null) {
                    gfx.itemTooltip(stack, mouseX, mouseY);
                }
            });
            list.setRows(lines);
        }

        private static void copy(SellPlan.Line line) {
            int stack = Math.max(1, line.sample().count());
            long dollars = Math.max(1, Math.floorDiv(line.unitCents() * stack, 100));
            Minecraft client = Minecraft.getInstance();
            client.keyboardHandler.setClipboard(Long.toString(dollars));
            client.gui.setOverlayMessage(Component.translatable("hugomarket.selllist.copied", Long.toString(dollars), stack), false);
        }

        @Override
        protected void layout() {
            int innerX = x + Theme.PADDING;
            int innerWidth = width - Theme.PADDING * 2;
            int top = contentTop();
            list.setBounds(innerX, top, innerWidth, y + height - Theme.PADDING - Theme.LINE - Theme.GAP - top);
        }

        @Override
        public void render(Gfx gfx, int mouseX, int mouseY) {
            super.render(gfx, mouseX, mouseY);
            String hint = list.rowCount() == 0 ? I18n.get("hugomarket.selllist.empty") : I18n.get("hugomarket.selllist.hint");
            gfx.textNoShadow(gfx.clip(hint, width - Theme.PADDING * 2), x + Theme.PADDING, y + height - Theme.PADDING - Theme.LINE + 2, Theme.TEXT_DIM);
        }

        private void renderRow(Gfx gfx, SellPlan.Line line, int rowX, int rowY, int rowWidth, int rowHeight, boolean hovered) {
            ItemStack stack = icons.get(line.sample());
            if (stack != null) {
                gfx.item(stack, rowX + 3, rowY + (rowHeight - ICON_SIZE) / 2);
            }
            String total = Money.compact(line.totalCents());
            String net = I18n.get("hugomarket.selllist.net", Money.compact(line.netCents()));
            int rightWidth = Math.max(gfx.textWidth(total), gfx.textWidth(net));
            int textX = rowX + ICON_SIZE + 6;
            int nameWidth = rowWidth - ICON_SIZE - 12 - rightWidth;
            String name = line.count() + "x " + ItemLabel.of(line.key(), line.sample().displayName());
            String detail = I18n.get("hugomarket.selllist.row", Money.format(line.unitCents()), Money.format(line.lowestUnitCents()), line.listings());
            gfx.textNoShadow(gfx.clip(name, nameWidth), textX, rowY + 2, Theme.TEXT);
            gfx.textNoShadow(gfx.clip(detail, nameWidth), textX, rowY + 11, Theme.TEXT_DIM);
            gfx.textNoShadow(total, rowX + rowWidth - gfx.textWidth(total) - 3, rowY + 2, Theme.GOLD);
            gfx.textNoShadow(net, rowX + rowWidth - gfx.textWidth(net) - 3, rowY + 11, Theme.PRICE);
        }
    }
}
