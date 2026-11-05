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
    void sanitizeFallsBackForEmptyTitles() {
        assertEquals("container", DumpFileNames.sanitize("  ***  "));
    }

    @Test
    void sanitizeTruncatesLongTitles() {
        String title = "x".repeat(100);
        assertEquals(40, DumpFileNames.sanitize(title).length());
    }

    @Test
    void containerFileNameHasStampAndTitle() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 5, 14, 3, 9, 7_000_000);
        assertEquals("20261105-140309-007_Orders.json", DumpFileNames.container(time, "Orders"));
    }

    @Test
    void chatFileNameIsPerDay() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 5, 23, 59, 0);
        assertEquals("chat-20261105.jsonl", DumpFileNames.chat(time));
    }
}
