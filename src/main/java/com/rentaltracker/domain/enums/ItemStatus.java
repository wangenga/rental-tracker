package com.rentaltracker.domain.enums;

public enum ItemStatus {
    available,
    rented,
    unlisted;

    public static ItemStatus safeValueOf(String value){
        try{
            return ItemStatus.valueOf(value.toLowerCase());
        } catch (IllegalArgumentException | NullPointerException e){
            return available;
        }
    }
}
