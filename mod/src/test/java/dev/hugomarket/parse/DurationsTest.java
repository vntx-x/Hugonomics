package dev.hugomarket.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurationsTest {
    @Test
    void parsesHugoDurations() {
        assertEquals(23 * 3600 + 33 * 60 + 55, Durations.parseSeconds("23h 33m 55s").getAsLong());
        assertEquals(5 * 86400, Durations.parseSeconds("5d 0h").getAsLong());
        assertEquals(42, Durations.parseSeconds("42s").getAsLong());
    }

    @Test
    void rejectsTextWithoutUnits() {
        assertTrue(Durations.parseSeconds("bald").isEmpty());
    }
}
