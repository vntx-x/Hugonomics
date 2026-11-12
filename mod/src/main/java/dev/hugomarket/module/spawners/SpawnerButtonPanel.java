package dev.hugomarket.module.spawners;

import dev.hugomarket.mixin.AbstractContainerScreenAccessor;
import dev.hugomarket.ui.Gfx;
import dev.hugomarket.ui.OverlayPanel;
import dev.hugomarket.ui.Theme;
import dev.hugomarket.ui.widget.Chip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class SpawnerButtonPanel extends OverlayPanel {
    private static final int HEIGHT = 16;
    private static final int WIDTH = 64;
    private static final int GAP_ABOVE = 2;
    private static final int ICON_SCALE_OFFSET = 2;

    private final Chip button;
    private final ItemStack icon = new ItemStack(Items.SPAWNER);

    SpawnerButtonPanel(AbstractContainerScreen<?> screen, Runnable open) {
        button = add(new Chip("     " + I18n.get("hugomarket.spawners.button"), () -> false, open));
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int right = accessor.hugomarket$leftPos() + accessor.hugomarket$imageWidth();
        setBounds(right - WIDTH, accessor.hugomarket$topPos() - HEIGHT - GAP_ABOVE, WIDTH, HEIGHT);
    }

    @Override
    protected void layout() {
        button.setBounds(x, y, width, height);
    }

    @Override
    public void render(Gfx gfx, int mouseX, int mouseY) {
        gfx.fill(x + 1, y + 1, width, height, Theme.PANEL_SHADOW);
        button.render(gfx, mouseX, mouseY);
        gfx.border(x, y, width, height, Theme.PANEL_BORDER);
        gfx.item(icon, x + ICON_SCALE_OFFSET, y);
    }
}
