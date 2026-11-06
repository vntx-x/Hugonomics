package dev.hugomarket.module.ahsearch;

import dev.hugomarket.model.Listing;
import dev.hugomarket.store.AhFocus;

import java.util.List;

public final class JumpCheck {
    public enum Outcome {
        FOUND,
        GONE,
        FURTHER
    }

    private JumpCheck() {
    }

    public static Outcome check(List<Listing> cheapestFirstPage, int pageCapacity, AhFocus focus) {
        long highest = 0;
        for (Listing listing : cheapestFirstPage) {
            if (listing.key().toString().equals(focus.key()) && listing.priceCents() == focus.priceCents()
                    && listing.item().count() == focus.count()) {
                return Outcome.FOUND;
            }
            highest = Math.max(highest, listing.priceCents());
        }
        boolean lastPage = cheapestFirstPage.size() < pageCapacity;
        return lastPage || highest > focus.priceCents() ? Outcome.GONE : Outcome.FURTHER;
    }
}
