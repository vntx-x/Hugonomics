package dev.hugomarket.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudPlacementTest {
    @Test
    void cornersAndClamping() {
        assertEquals(4, HudPlacement.position(0, 400, 100));
        assertEquals(296, HudPlacement.position(1, 400, 100));
        assertEquals(296, HudPlacement.position(5, 400, 100));
        assertEquals(4, HudPlacement.position(-1, 400, 100));
        assertEquals(4, HudPlacement.position(0.5, 50, 100));
    }

    @Test
    void fractionRoundTripsAndStaysRelativeOnResize() {
        double fraction = HudPlacement.fraction(150, 400, 100);
        assertEquals(150, HudPlacement.position(fraction, 400, 100));
        assertEquals(0.5, HudPlacement.fraction(150, 400, 100), 1e-9);
        assertEquals(1, HudPlacement.fraction(900, 400, 100));
        assertEquals(0, HudPlacement.fraction(-50, 400, 100));
        assertEquals(450, HudPlacement.position(fraction, 1000, 100));
    }
}
