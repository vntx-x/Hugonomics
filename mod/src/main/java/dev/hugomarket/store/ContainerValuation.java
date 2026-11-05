package dev.hugomarket.store;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.SlotView;
import dev.hugomarket.parse.HugoLore;
import dev.hugomarket.parse.ItemKeyFactory;

import java.util.List;
import java.util.Optional;

public record ContainerValuation(
        long hugoWorthCents,
        long hugoBaseCents,
        long ahLowestCents,
        long ahMedianCents,
        int items,
        int valuedByAh,
        int unvaluedByAh
) {
    public static ContainerValuation of(ContainerView view, MarketSession market) {
        Accumulator acc = new Accumulator();
        for (SlotView slot : view.slots()) {
            if (slot.playerInventory() || slot.isEmpty()) {
                continue;
            }
            acc.add(slot.item(), market, true);
        }
        return acc.result();
    }

    public static ContainerValuation ofItem(ItemView item, MarketSession market) {
        Accumulator acc = new Accumulator();
        acc.add(item, market, true);
        return acc.result();
    }

    public static ContainerValuation ofItems(List<ItemView> items, MarketSession market) {
        Accumulator acc = new Accumulator();
        for (ItemView item : items) {
            acc.add(item, market, true);
        }
        return acc.result();
    }

    public boolean hasAhValue() {
        return valuedByAh > 0;
    }

    private static final class Accumulator {
        private long hugoWorth;
        private long hugoBase;
        private long ahLowest;
        private long ahMedian;
        private int items;
        private int valued;
        private int unvalued;

        void add(ItemView item, MarketSession market, boolean countHugo) {
            items++;
            if (countHugo) {
                Optional<HugoLore.Worth> worth = HugoLore.worth(item.lore());
                if (worth.isPresent()) {
                    hugoWorth += worth.get().valueCents();
                    hugoBase += worth.get().baseCents();
                }
            }
            PriceStats stats = market.stats(ItemKeyFactory.of(item));
            if (stats != null) {
                valued++;
                ahLowest += stats.lowestUnitCents() * item.count();
                ahMedian += stats.medianUnitCents() * item.count();
            } else if (item.hasContents()) {
                for (ItemView inner : item.contents()) {
                    add(inner, market, false);
                }
            } else {
                unvalued++;
            }
        }

        ContainerValuation result() {
            return new ContainerValuation(hugoWorth, hugoBase, ahLowest, ahMedian, items, valued, unvalued);
        }
    }
}
