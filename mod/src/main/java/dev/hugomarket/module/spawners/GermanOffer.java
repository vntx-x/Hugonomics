package dev.hugomarket.module.spawners;

import dev.hugomarket.model.Money;
import dev.hugomarket.ui.GermanItemNames;

final class GermanOffer {
    private GermanOffer() {
    }

    // Hugo is a German server, so the chat offer is always German whatever language the client runs in.
    static String text(boolean selling, long count, String type, long unitCents) {
        return GermanItemNames.text(selling ? "hugomarket.spawners.offer.sell" : "hugomarket.spawners.offer.buy",
                count, SpawnerNames.german(type), Money.format(unitCents), Money.format(unitCents * count));
    }
}
