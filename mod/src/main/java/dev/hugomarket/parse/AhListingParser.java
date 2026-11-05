package dev.hugomarket.parse;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.HugoControl;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.model.SlotView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class AhListingParser {
    private static final String SORT_HEADER = "Sortierung:";
    private static final String STACK_HEADER = "Stack-Filter:";

    public record AhFilter(String sort, String stackFilter) {
    }

    public record AhTotals(long spentCents, long earnedCents) {
    }

    public record AhPage(List<Listing> listings, AhFilter filter, AhTotals totals) {
    }

    private final HugoGuiRules rules;

    public AhListingParser(HugoGuiRules rules) {
        this.rules = rules;
    }

    public AhPage parse(ContainerView view) {
        HugoGuiRules.ScreenRule rule = rules.rule(ScreenKind.AH_LIST);
        List<Listing> listings = new ArrayList<>();
        AhFilter filter = null;
        AhTotals totals = null;
        for (SlotView slot : view.slots()) {
            if (slot.playerInventory() || slot.isEmpty()) {
                continue;
            }
            ItemView item = slot.item();
            HugoControl control = rules.controlOf(item);
            if (control == HugoControl.FILTER) {
                filter = new AhFilter(
                        HugoLore.selectedOption(item.lore(), SORT_HEADER).orElse(null),
                        HugoLore.selectedOption(item.lore(), STACK_HEADER).orElse(null));
                continue;
            }
            if (control == HugoControl.OWN) {
                OptionalLong spent = HugoLore.spentTotal(item.lore());
                OptionalLong earned = HugoLore.earnedTotal(item.lore());
                if (spent.isPresent() && earned.isPresent()) {
                    totals = new AhTotals(spent.getAsLong(), earned.getAsLong());
                }
                continue;
            }
            if (control != null || !rule.isContentSlot(slot.index())) {
                continue;
            }
            listing(slot, item).ifPresent(listings::add);
        }
        return new AhPage(List.copyOf(listings), filter, totals);
    }

    public static Optional<Listing> listing(SlotView slot, ItemView item) {
        OptionalLong price = HugoLore.price(item.lore());
        Optional<String> seller = HugoLore.seller(item.lore());
        if (price.isEmpty() || seller.isEmpty()) {
            return Optional.empty();
        }
        long expires = HugoLore.expiresInSeconds(item.lore()).orElse(-1);
        return Optional.of(new Listing(slot.index(), item, ItemKeyFactory.of(item), price.getAsLong(), seller.get(), expires));
    }
}
