package com.rentaltracker.transport;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DisplayTest {

    @Test
    void time_formatsAsYearMonthDayHourMinute() {
        LocalDateTime t = LocalDateTime.of(2025, 3, 9, 14, 5);
        assertEquals("2025-03-09 14:05", Display.time(t));
    }

    @Test
    void time_padsSingleDigits() {
        LocalDateTime t = LocalDateTime.of(2024, 1, 2, 3, 4);
        assertEquals("2024-01-02 03:04", Display.time(t));
    }

    @Test
    void row_alignsLabelToFourteenChars() {
        String row = Display.row("start", "x");
        assertEquals("start          x", row); // "start" padded to 14 + space + value
    }

    @Test
    void row_acceptsNonStringValues() {
        assertTrue(Display.row("cost", 42).endsWith("42"));
    }
}