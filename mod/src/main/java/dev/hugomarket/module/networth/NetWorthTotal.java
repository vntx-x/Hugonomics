package dev.hugomarket.module.networth;

import dev.hugomarket.container.ItemViews;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.store.ContainerValuation;
import dev.hugomarket.store.MarketSession;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class NetWorthTotal {
    private NetWorthTotal() {
    }

    static long inventory(MarketSession market) {
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

    static long total(NetWorthStore store, MarketSession market) {
        return inventory(market) + store.balance().cents() + store.enderchest().cents() + store.auctions().cents();
    }
}
