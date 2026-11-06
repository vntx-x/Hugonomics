package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Listing;
import dev.hugomarket.store.AhFocus;

import java.util.List;

public final class JumpCheck {
    public enum Outcome {
        FOUND,
        GONE,
        NEXT_PAGE
    }

    private JumpCheck() {
    }

    public static Outcome check(List<Listing> cheapestFirstPage, boolean hasNextPage, AhFocus focus) {
        for (Listing listing : cheapestFirstPage) {
            if (listing.key().toString().equals(focus.key()) && listing.priceCents() == focus.priceCents()
                    && listing.item().count() == focus.count()) {
                return Outcome.FOUND;
            }
        }
        if (cheapestFirstPage.isEmpty()) {
            return Outcome.GONE;
        }
        Listing first = cheapestFirstPage.get(0);
        long focusUnit = Math.round((double) focus.priceCents() / Math.max(1, focus.count()));
        boolean passed = first.priceCents() > focus.priceCents() && first.unitPriceCents() > focusUnit;
        if (passed || !hasNextPage) {
            return Outcome.GONE;
        }
        return Outcome.NEXT_PAGE;
    }
}
