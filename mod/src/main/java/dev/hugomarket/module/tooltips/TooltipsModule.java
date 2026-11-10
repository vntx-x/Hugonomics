package dev.hugomarket.module.tooltips;

import dev.hugomarket.container.ItemViews;
import dev.hugomarket.core.Module;
import dev.hugomarket.core.ModuleContext;
import dev.hugomarket.store.Fees;
import dev.hugomarket.store.MarketSession;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.WeakHashMap;

public final class TooltipsModule implements Module {
    private record Cached(int contentHash, int count, int marketVersion, List<Component> lines) {
    }

    private final WeakHashMap<ItemStack, Cached> cache = new WeakHashMap<>();
    private ModuleContext context;
    private Fees fees;
    private boolean enabled;

    @Override
    public String id() {
        return "tooltips";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public void onEnable(ModuleContext context) {
        this.context = context;
        fees = Fees.load();
        enabled = true;
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, flag, lines) -> append(stack, lines));
    }

    @Override
    public void onDisable() {
        enabled = false;
        cache.clear();
    }

    private void append(ItemStack stack, List<Component> lines) {
        if (!enabled || stack.isEmpty() || !context.server().active()) {
            return;
        }
        MarketSession market = context.market();
        int hash = ItemStack.hashItemAndComponents(stack);
        Cached cached = cache.get(stack);
        if (cached == null || cached.contentHash() != hash || cached.count() != stack.getCount() || cached.marketVersion() != market.version()) {
            cached = new Cached(hash, stack.getCount(), market.version(), List.copyOf(TooltipLines.build(ItemViews.of(stack), market, fees)));
            cache.put(stack, cached);
        }
        lines.addAll(cached.lines());
    }
}
