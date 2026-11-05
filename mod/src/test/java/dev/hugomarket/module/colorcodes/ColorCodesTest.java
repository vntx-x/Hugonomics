package dev.hugomarket.module.colorcodes;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorCodesTest {
    @Test
    void parsesLegacyColorsAndFormats() {
        List<ColorCodes.Run> runs = ColorCodes.parse("&aHallo &l&bWelt&r!");
        assertEquals(3, runs.size());
        assertEquals("Hallo ", runs.get(0).text());
        assertEquals(0x55FF55, runs.get(0).color());
        assertFalse(runs.get(0).bold());
        assertEquals("Welt", runs.get(1).text());
        assertEquals(0x55FFFF, runs.get(1).color());
        assertFalse(runs.get(1).bold());
        assertEquals("!", runs.get(2).text());
        assertEquals(-1, runs.get(2).color());
    }

    @Test
    void colorCodeResetsFormatting() {
        List<ColorCodes.Run> runs = ColorCodes.parse("&a&lFett&bNormal&rOhne");
        assertTrue(runs.get(0).bold());
        assertEquals(0x55FF55, runs.get(0).color());
        assertFalse(runs.get(1).bold());
        assertEquals(0x55FFFF, runs.get(1).color());
        assertEquals(-1, runs.get(2).color());
        assertFalse(ColorCodes.parse("&l&aFett").get(0).bold());
    }

    @Test
    void keepsUnknownCodesAndHexAsText() {
        assertEquals("Preis & Co &z &#FF8800Test", ColorCodes.strip("Preis & Co &z &#FF8800Test"));
        assertEquals(1, ColorCodes.parse("Preis & Co &z &#FF8800Test").size());
    }

    @Test
    void countsVisibleCharactersOnly() {
        assertEquals(9, ColorCodes.visibleLength("&l&dHugo &aFarm"));
        assertEquals(0, ColorCodes.visibleLength("&a&l"));
    }

    @Test
    void acceptsUpperCaseCodes() {
        assertEquals(0xFF5555, ColorCodes.parse("&CRot").get(0).color());
        assertTrue(ColorCodes.parse("&LFett").get(0).bold());
    }
}
