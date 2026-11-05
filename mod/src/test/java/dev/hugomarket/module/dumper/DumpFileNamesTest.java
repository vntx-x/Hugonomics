package dev.hugomarket.module.dumper;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DumpFileNamesTest {
    @Test
    void sanitizeReplacesRunsOfSpecialCharacters() {
        assertEquals("Auktionshaus_1_12", DumpFileNames.sanitize("Auktionshaus (1/12)"));
    }

    @Test
    void sanitizeKeepsUmlautsAndDashes() {
        assertEquals("Verkäufe-Übersicht", DumpFileNames.sanitize("Verkäufe-Übersicht"));
    }

    @Test
    void sanitizeDropsCustomFontGlyphs() {
        assertEquals("gui", DumpFileNames.sanitize("ꐭ"));
    }

    @Test
    void sanitizeFallsBackForEmptyTitles() {
        assertEquals("gui", DumpFileNames.sanitize("  ***  "));
    }

    @Test
    void sanitizeTruncatesLongTitles() {
        String title = "x".repeat(100);
        assertEquals(40, DumpFileNames.sanitize(title).length());
    }

    @Test
    void containerFileNameHasStampTitleAndSlots() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 5, 14, 3, 9, 7_000_000);
        assertEquals("20261105-140309-007_Orders_54.json", DumpFileNames.container(time, "Orders", 54));
    }

    @Test
    void chatFileNameIsPerDay() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 5, 23, 59, 0);
        assertEquals("chat-20261105.jsonl", DumpFileNames.chat(time));
    }
}
