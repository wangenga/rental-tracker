package com.rentaltracker.domain.enums;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.rentaltracker.domain.enums.ItemStatus.*;

class ItemStatusTest {

    private static final Set<String> ALLOWED = Set.of(
            "available>rented", "available>unlisted",
            "rented>available", "rented>unlisted",
            "unlisted>available");

    @Test
    void everyMoveIsAllowedOrRejectedExactlyAsListed() {
        for (ItemStatus from : values()) {
            for (ItemStatus to : values()) {
                boolean expected = ALLOWED.contains(from + ">" + to);
                assertEquals(expected, from.canTransitionTo(to), from + " -> " + to);
            }
        }
    }

    @Test
    void transitionToReturnsTheNewStatusWhenAllowed() {
        assertEquals(rented, available.transitionTo(rented));
    }

    @Test
    void transitionToThrowsWhenNotAllowed() {
        assertThrows(IllegalStateException.class, () -> unlisted.transitionTo(rented));
        assertThrows(IllegalStateException.class, () -> available.transitionTo(available));
    }

    @Test
    void fromDbReadsTheStoredLowercaseText() {
        assertEquals(available, fromDb("available"));
        assertEquals(rented, fromDb("rented"));
        assertEquals(unlisted, fromDb("unlisted"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"broken", "", "  "})
    void fromDbRejectsUnknownText(String text) {
        assertThrows(IllegalArgumentException.class, () -> fromDb(text));
    }

    @ParameterizedTest
    @NullSource
    void fromDbRejectsNull(String text) {
        assertThrows(IllegalArgumentException.class, () -> fromDb(text));
    }
}