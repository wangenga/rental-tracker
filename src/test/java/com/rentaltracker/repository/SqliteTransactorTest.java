package com.rentaltracker.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.exception.RepositoryException;
import com.rentaltracker.repository.exception.SqlExceptionTranslator;

class SqliteTransactorTest {

    @TempDir Path tempDir;
    Database db;
    SqliteTransactor tx;

    @BeforeEach
    void setUp() {
        db = new Database(tempDir.resolve("test.db").toString());
        tx = new SqliteTransactor(db);
    }

    @AfterEach
    void tearDown() { db.close(); }

    private void exec(String sql) {
        try (Statement st = db.connection().createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw SqlExceptionTranslator.translate("test sql", e);
        }
    }

    private int userCount() throws SQLException {
        try (Statement st = db.connection().createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM users")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static final String ADD_A = "INSERT INTO users (username) VALUES ('a')";

    @Test
    void commitsAndReturnsTheResultWhenWorkSucceeds() throws SQLException {
        String result = tx.inTransaction(() -> {
            exec(ADD_A);
            return "done";
        });

        assertEquals("done", result);
        assertEquals(1, userCount());
    }

    @Test
    void rollsBackAndRethrowsWhenWorkThrows() throws SQLException {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> tx.runInTransaction(() -> {
                    exec(ADD_A);
                    throw new IllegalStateException("boom");
                }));

        assertEquals("boom", e.getMessage());
        assertEquals(0, userCount());
    }

    @Test
    void rollsBackEarlierWritesWhenALaterWriteFails() throws SQLException {
        // the second insert breaks the UNIQUE rule, so the first must disappear too
        assertThrows(RepositoryException.class,
                () -> tx.runInTransaction(() -> {
                    exec(ADD_A);
                    exec(ADD_A);
                }));

        assertEquals(0, userCount());
    }

    @Test
    void autoCommitIsRestoredAfterSuccessAndAfterFailure() throws SQLException {
        tx.runInTransaction(() -> exec(ADD_A));
        assertTrue(db.connection().getAutoCommit());

        assertThrows(RuntimeException.class,
                () -> tx.runInTransaction(() -> { throw new RuntimeException("x"); }));
        assertTrue(db.connection().getAutoCommit());
    }

    @Test
    void canBeUsedAgainAfterAFailure() throws SQLException {
        assertThrows(RuntimeException.class,
                () -> tx.runInTransaction(() -> { throw new RuntimeException("x"); }));

        tx.runInTransaction(() -> exec(ADD_A));

        assertEquals(1, userCount());
    }

    @Test
    void innerTransactionJoinsTheOuterOneAndRollsBackWithIt() throws SQLException {
        assertThrows(IllegalStateException.class,
                () -> tx.runInTransaction(() -> {
                    tx.runInTransaction(() -> exec(ADD_A));   // inner "succeeds"
                    throw new IllegalStateException("outer fails");
                }));

        assertEquals(0, userCount());
        assertTrue(db.connection().getAutoCommit());
    }

    @Test
    void nestedTransactionsCommitTogetherWhenNothingFails() throws SQLException {
        tx.runInTransaction(() -> {
            tx.runInTransaction(() -> exec(ADD_A));
            exec("INSERT INTO users (username) VALUES ('b')");
        });

        assertEquals(2, userCount());
        assertTrue(db.connection().getAutoCommit());
    }

    @Test
    void closedDatabaseThrowsRepositoryException() {
        db.close();

        assertThrows(RepositoryException.class, () -> tx.runInTransaction(() -> { }));
    }
}