package com.rentaltracker.repository;

import java.util.function.Supplier;

public interface Transactor {
    
    /** Runs the work as one unit: commit if it returns, roll back if it throws. */

    <T> T inTransaction(Supplier<T> work);

    /** for work with no result */
    default void runInTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
