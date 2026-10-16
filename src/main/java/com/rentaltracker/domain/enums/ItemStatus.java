package com.rentaltracker.domain.enums;

public enum ItemStatus {
    available,
    rented,
    unlisted;

    /** Allowed moves. Verify these against the state-machine diagram in the spec. */
    public boolean canTransitionTo(ItemStatus next) {
        return switch (this) {
            case available -> next == rented || next == unlisted;
            case rented    -> next == available || next == unlisted;
            case unlisted  -> next == available;
        };
    }

    /** Last-line guard in code. Services check canTransitionTo first to give a friendly message. */
    public ItemStatus transitionTo(ItemStatus next) {
        if (!canTransitionTo(next)) {
            throw new IllegalStateException("Cannot change item from " + this + " to " + next);
        }
        return next;
    }

    /** safe value of turns any unknown or null status into available which is the opposite of what we want*/
    

    /** Strict: an unknown or null value is an error, never a guess. */
    public static ItemStatus fromDb(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Item status is null");
        }
        return valueOf(value.toLowerCase());
    }
}
