package com.rentaltracker.domain.enums;

public enum RentalStatus {
    ACTIVE, CLOSED;

    public String toDb() { return name().toLowerCase(); }
    public static RentalStatus fromDb(String v) { return valueOf(v.toUpperCase()); }
}
