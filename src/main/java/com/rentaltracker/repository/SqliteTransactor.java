package com.rentaltracker.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.exception.SqlExceptionTranslator;

public class SqliteTransactor implements Transactor {

    private final Database db;
    private boolean active = false;   // true while a transaction is open

    public SqliteTransactor(Database db) {
        this.db = db;
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        if (active) {
            return work.get();   // already inside one: join it, the outer call decides
        }

        Connection conn = db.connection();
        boolean started = false;
        try {
            conn.setAutoCommit(false);
            started = true;
            active = true;
            try {
                T result = work.get();
                conn.commit();
                return result;
            } catch (RuntimeException | SQLException e) {
                rollbackQuietly(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("transaction", e);
        } finally {
            active = false;
            if (started) {
                restoreAutoCommit(conn);
            }
        }
    }

    private void rollbackQuietly(Connection conn, Exception original) {
        try {
            conn.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);   // keep the real cause visible
        }
    }

    private void restoreAutoCommit(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ignored) {
            // nothing useful to do, and throwing here would hide the real error
        }
    }
}