package dev.hugomarket.store;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeesTest {
    private final Fees fees = Fees.load();

    @Test
    void listingFeeIsOnePercent() {
        assertEquals(60_000, fees.listingFee(6_000_000));
        assertEquals(1, fees.listingFee(100));
    }

    @Test
    void saleTaxTiersFollowHugoTable() {
        assertEquals(1.0, fees.saleTaxPercent(500_000));
        assertEquals(1.0, fees.saleTaxPercent(1_000_000));
        assertEquals(2.0, fees.saleTaxPercent(1_000_100));
        assertEquals(2.0, fees.saleTaxPercent(100_000_000));
        assertEquals(3.0, fees.saleTaxPercent(100_000_100));
        assertEquals(300_000_000, fees.saleTax(100_000_000_00L));
        assertEquals(97_000_000_00L, fees.netAfterSale(100_000_000_00L));
    }

    @Test
    void orderCreationFeeMatchesDump() {
        assertEquals(6_400, fees.orderCreationFee(640_000));
    }
}
