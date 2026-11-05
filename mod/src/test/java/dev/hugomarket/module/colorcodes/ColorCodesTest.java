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
        assertEquals(1, ColorCodes.parse("&l&aFett").size());
        assertFalse(ColorCodes.parse("&l&aFett").get(0).bold());
    }

    @Test
    void parsesHexCodes() {
        List<ColorCodes.Run> runs = ColorCodes.parse("&#FF8800Gold&#00ff00Grün");
        assertEquals(0xFF8800, runs.get(0).color());
        assertEquals(0x00FF00, runs.get(1).color());
        assertEquals("Grün", runs.get(1).text());
    }

    @Test
    void keepsIncompleteCodesAsText() {
        assertEquals("Preis & Co &z &#12", ColorCodes.strip("Preis & Co &z &#12"));
        assertEquals(1, ColorCodes.parse("Preis & Co &z &#12").size());
    }

    @Test
    void countsVisibleCharactersOnly() {
        assertEquals(9, ColorCodes.visibleLength("&l&#C77DFFHugo &aFarm"));
    }

    @Test
    void gradientColorsEveryLetterAndSkipsSpaces() {
        String out = ColorCodes.gradient("&cab c", 0x000000, 0xFFFFFF);
        assertEquals("&#000000a&#808080b &#FFFFFFc", out);
        assertEquals("ab c", ColorCodes.strip(out));
    }

    @Test
    void gradientOfSingleLetterUsesStartColor() {
        assertEquals("&#112233x", ColorCodes.gradient("x", 0x112233, 0xFFFFFF));
    }

    @Test
    void parsesHexInput() {
        assertEquals(0xC77DFF, ColorCodes.parseHex("#c77dff"));
        assertEquals(0xC77DFF, ColorCodes.parseHex("C77DFF"));
        assertEquals(-1, ColorCodes.parseHex("#fff"));
        assertEquals(-1, ColorCodes.parseHex("#zzzzzz"));
    }
}
