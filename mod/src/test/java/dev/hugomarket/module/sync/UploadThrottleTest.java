package dev.hugomarket.module.sync;

import dev.hugomarket.model.ItemView;
import dev.hugomarket.model.Listing;
import dev.hugomarket.model.ListingStatus;
import dev.hugomarket.parse.ItemKeyFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UploadThrottleTest {
    private static Listing listing(String seller, long priceCents, long expiresIn, boolean own) {
        ItemView item = new ItemView("minecraft:diamond", 4, "minecraft:diamond", List.of(), Map.of(), Map.of(), null, null,
                List.of(), null, null, Map.of(), List.of());
        return new Listing(0, item, ItemKeyFactory.of(item), priceCents, seller, expiresIn, ListingStatus.ACTIVE, own);
    }

    @Test
    void sendsEachListingOncePerMinute() {
        UploadThrottle throttle = new UploadThrottle(60_000);
        List<Listing> page = List.of(listing("a", 1000, 3600, false), listing("b", 2000, 3600, false));
        assertEquals(2, throttle.filter(page, 10_000).size());
        assertEquals(0, throttle.filter(List.of(listing("a", 1000, 3599, false), listing("b", 2000, 3599, false)), 11_000).size());
        assertEquals(0, throttle.filter(List.of(listing("a", 1000, 3570, false)), 40_000).size());
        assertEquals(1, throttle.filter(List.of(listing("a", 1000, 3540, false)), 70_000).size());
    }

    @Test
    void treatsChangedPriceAsNewListing() {
        UploadThrottle throttle = new UploadThrottle(60_000);
        throttle.filter(List.of(listing("a", 1000, 3600, false)), 10_000);
        assertEquals(1, throttle.filter(List.of(listing("a", 900, 3599, false)), 11_000).size());
    }

    @Test
    void toleratesExpiryJitterAcrossBuckets() {
        UploadThrottle throttle = new UploadThrottle(60_000);
        throttle.filter(List.of(listing("a", 1000, 3609, false)), 10_000);
        assertEquals(0, throttle.filter(List.of(listing("a", 1000, 3597, false)), 11_000).size());
    }

    @Test
    void skipsOwnAndInactiveListings() {
        UploadThrottle throttle = new UploadThrottle(60_000);
        assertEquals(0, throttle.filter(List.of(listing("Du", 1000, 3600, true)), 10_000).size());
    }
}
