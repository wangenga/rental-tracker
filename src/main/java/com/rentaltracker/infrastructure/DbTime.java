package com.rentaltracker.infrastructure;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** The one place that defines how timestamps are stored: yyyy-MM-dd HH:mm */
public final class DbTime {

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DbTime() {}

    public static String format(LocalDateTime time) {
        return time.format(FORMAT);
    }

    public static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text, FORMAT);
    }
}