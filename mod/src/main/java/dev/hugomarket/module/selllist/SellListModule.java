package dev.hugomarket.module.selllist;

import dev.hugomarket.container.ItemViews;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.parse.ItemKeyFactory;
import dev.hugomarket.store.Fees;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class SellListModule implements Module {
    private static final int MAIN_SLOTS = 36;

    private ModuleContext context;
    private Fees fees;

    @Override
    public String id() {
        return "selllist";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        fees = Fees.load();
        context.menu().add("hugomarket.selllist.open", this::open);
    }

    @Override
    public void onDisable() {
    }

    private void open() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        Inventory inventory = client.player.getInventory();
        List<ItemView> items = new ArrayList<>(MAIN_SLOTS);
        List<ItemStack> stacks = new ArrayList<>(MAIN_SLOTS);
        for (int slot = 0; slot < MAIN_SLOTS; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                items.add(ItemViews.of(stack));
                stacks.add(stack.copy());
            }
        }
        List<SellPlan.Line> lines = SellPlan.build(items, ItemKeyFactory::of, context.market()::stats, fees);
        client.setScreen(new SellListScreen(client.screen, lines, items, stacks));
    }
}
