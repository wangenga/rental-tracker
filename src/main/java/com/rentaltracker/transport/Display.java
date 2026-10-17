package com.rentaltracker.transport;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Display {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Display() {}

    public static String time(LocalDateTime time) { return TIME.format(time); }

    /** "label   value" in two aligned columns. Used for list rows and detail fields. */
    public static String row(String label, Object value) {
        return String.format("%-14s %s", label, value);
    }
}
