package dev.hugomarket.module.tooltips;

import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Money;
import dev.hugomarket.parse.HugoLore;
import dev.hugomarket.parse.ItemKeyFactory;
import dev.hugomarket.store.ContainerValuation;
import dev.hugomarket.store.MarketSession;
import dev.hugomarket.store.OrderBook;
import dev.hugomarket.store.PriceStats;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

final class TooltipLines {
    private TooltipLines() {
    }

    static List<Component> build(ItemView item, MarketSession market) {
        List<Component> lines = new ArrayList<>(4);
        OptionalLong listingPrice = HugoLore.price(item.lore());
        if (listingPrice.isPresent() && item.count() > 1) {
            long unit = Math.round((double) listingPrice.getAsLong() / item.count());
            lines.add(line("hugomarket.tooltip.unit_price", Money.format(unit)));
        }
        PriceStats stats = market.stats(ItemKeyFactory.of(item));
        OrderBook.BestOrder order = market.orders().best(ItemKeyFactory.of(item).toString());
        if (order != null) {
            lines.add(line("hugomarket.tooltip.order", Money.format(order.unitPriceCents()), order.openUnits()));
        }
        if (stats != null) {
            lines.add(line("hugomarket.tooltip.ah", Money.format(stats.lowestUnitCents()), Money.format(stats.medianUnitCents()), stats.listings()));
            if (item.count() > 1) {
                lines.add(line("hugomarket.tooltip.ah_stack", Money.format(stats.lowestUnitCents() * item.count())));
            }
        } else if (item.hasContents()) {
            ContainerValuation contents = ContainerValuation.ofItem(item, market);
            if (contents.hasAhValue()) {
                lines.add(line("hugomarket.tooltip.contents", Money.compact(contents.ahLowestCents()), contents.valuedByAh(), contents.unvaluedByAh()));
            }
        }
        return lines;
    }

    private static Component line(String key, Object... args) {
        return Component.literal(I18n.get(key, args)).withStyle(ChatFormatting.DARK_PURPLE);
    }
}
