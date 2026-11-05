package dev.hugomarket.parse;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Order;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.model.SlotView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class OrderListParser {
    private static final String OWNER_PREFIX = "Order von ";

    private final HugoGuiRules rules;

    public OrderListParser(HugoGuiRules rules) {
        this.rules = rules;
    }

    public List<Order> parse(ContainerView view) {
        HugoGuiRules.ScreenRule rule = rules.rule(ScreenKind.ORDER_LIST);
        List<Order> orders = new ArrayList<>();
        for (SlotView slot : view.slots()) {
            if (slot.playerInventory() || slot.isEmpty() || !rule.isContentSlot(slot.index())) {
                continue;
            }
            if (rules.controlOf(slot.item()) != null) {
                continue;
            }
            order(slot, slot.item()).ifPresent(orders::add);
        }
        return List.copyOf(orders);
    }

    public static Optional<Order> order(SlotView slot, ItemView item) {
        if (!item.displayName().startsWith(OWNER_PREFIX)) {
            return Optional.empty();
        }
        OptionalLong unitPrice = HugoLore.unitPrice(item.lore());
        Optional<HugoLore.Progress> delivered = HugoLore.delivered(item.lore());
        Optional<HugoLore.PaidOut> paid = HugoLore.paidOut(item.lore());
        if (unitPrice.isEmpty() || delivered.isEmpty() || paid.isEmpty()) {
            return Optional.empty();
        }
        String owner = item.displayName().substring(OWNER_PREFIX.length()).trim();
        return Optional.of(new Order(slot.index(), item, ItemKeyFactory.of(item), owner, unitPrice.getAsLong(),
                delivered.get().done(), delivered.get().total(), paid.get().paidCents(), paid.get().totalCents()));
    }
}
