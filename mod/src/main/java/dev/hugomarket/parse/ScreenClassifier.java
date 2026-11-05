package dev.hugomarket.parse;

import dev.hugomarket.model.ContainerView;
import dev.hugomarket.model.ScreenKind;
import dev.hugomarket.model.SlotView;

public final class ScreenClassifier {
    private final HugoGuiRules rules;

    public ScreenClassifier(HugoGuiRules rules) {
        this.rules = rules;
    }

    public ScreenKind classify(ContainerView view) {
        if ("inventory".equals(view.menuType())) {
            return ScreenKind.PLAYER_INVENTORY;
        }
        String secured = securedTag(view);
        for (HugoGuiRules.ScreenRule rule : rules.screens()) {
            if (!rule.title().equals(view.title()) || !rule.menuType().equals(view.menuType())) {
                continue;
            }
            if (rule.secured() != null && !rule.secured().equals(secured)) {
                continue;
            }
            return rule.kind();
        }
        return secured != null ? ScreenKind.UNKNOWN_HUGO : ScreenKind.VANILLA_CONTAINER;
    }

    private static String securedTag(ContainerView view) {
        for (SlotView slot : view.slots()) {
            if (slot.playerInventory() || slot.isEmpty()) {
                continue;
            }
            String tag = slot.item().secured();
            if (tag != null) {
                return tag;
            }
        }
        return null;
    }
}
