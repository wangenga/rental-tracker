package com.rentaltracker;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchemaTest {

    @TempDir Path tempDir;
    Connection conn;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection(
                "jdbc:sqlite:" + tempDir.resolve("schema-test.db") + "?foreign_keys=on");
        loadSchema();
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    // ---------- helpers ----------

    private void loadSchema() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/schema.sql")) {
            assertNotNull(in, "schema.sql not found on classpath");
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement st = conn.createStatement()) {
                for (String sql : script.split(";")) {
                    if (!sql.isBlank()) {
                        st.execute(sql);
                    }
                }
            }
        }
    }

    private void exec(String sql) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private void assertFails(String expectedMessage, String sql) {
        SQLException e = assertThrows(SQLException.class, () -> exec(sql));
        assertTrue(e.getMessage().contains(expectedMessage),
                "Expected '" + expectedMessage + "' but got: " + e.getMessage());
    }

    private long count(String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void seedOwnerRenterAndItem() throws SQLException {
        exec("INSERT INTO users(username) VALUES ('liisa')");
        exec("INSERT INTO users(username) VALUES ('meelis')");
        exec("INSERT INTO listed_items(owner_id, item_name, description, cost_per_day) "
                + "VALUES (1, 'Ladder', '6-step', 5)");
    }

    private static final String ACTIVE_RENTAL =
            "INSERT INTO rentals(item_id, renter_id, start_time, end_time) "
            + "VALUES (1, 2, '2026-06-16 14:30', '2026-06-19 14:30')";

    // ---------- structure and defaults ----------

    @Test
    void createsExactlyTheThreeTables() throws SQLException {
        assertEquals(3, count("SELECT count(*) FROM sqlite_master "
                + "WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"));
        assertEquals(3, count("SELECT count(*) FROM sqlite_master WHERE type = 'table' "
                + "AND name IN ('users', 'listed_items', 'rentals')"));
    }

    @Test
    void runningTheSchemaTwiceIsHarmless() throws Exception {
        seedOwnerRenterAndItem();

        loadSchema();   // what happens on every app start

        assertEquals(2, count("SELECT count(*) FROM users"));
        assertEquals(1, count("SELECT count(*) FROM listed_items"));
    }

    @Test
    void defaultsAreApplied() throws SQLException {
        seedOwnerRenterAndItem();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT status, created_at FROM listed_items")) {
            rs.next();
            assertEquals("available", rs.getString("status"));
            assertTrue(rs.getString("created_at").matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"),
                    "created_at should look like 2026-06-16 14:30 but was " + rs.getString("created_at"));
        }
    }

    // ---------- users ----------

    @Test
    void duplicateUsernameViolatesUnique() throws SQLException {
        exec("INSERT INTO users(username) VALUES ('liisa')");
        assertFails("UNIQUE constraint failed", "INSERT INTO users(username) VALUES ('liisa')");
    }

    @Test
    void blankUsernameViolatesCheck() {
        assertFails("CHECK constraint failed", "INSERT INTO users(username) VALUES ('   ')");
    }

    @Test
    void nullUsernameViolatesNotNull() {
        assertFails("NOT NULL constraint failed", "INSERT INTO users(username) VALUES (NULL)");
    }

    // ---------- listed_items ----------

    @Test
    void itemWithoutNameViolatesNotNull() throws SQLException {
        exec("INSERT INTO users(username) VALUES ('liisa')");
        assertFails("NOT NULL constraint failed",
                "INSERT INTO listed_items(owner_id, item_name, description, cost_per_day) "
                        + "VALUES (1, NULL, 'x', 5)");
    }

    @Test
    void blankItemNameViolatesCheck() throws SQLException {
        exec("INSERT INTO users(username) VALUES ('liisa')");
        assertFails("CHECK constraint failed",
                "INSERT INTO listed_items(owner_id, item_name, description, cost_per_day) "
                        + "VALUES (1, '  ', 'x', 5)");
    }

    @Test
    void itemWithBadStatusViolatesCheck() throws SQLException {
        exec("INSERT INTO users(username) VALUES ('liisa')");
        assertFails("CHECK constraint failed",
                "INSERT INTO listed_items(owner_id, item_name, description, cost_per_day, status) "
                        + "VALUES (1, 'Drill', 'x', 3, 'broken')");
    }

    @Test
    void zeroCostViolatesCheck() throws SQLException {
        exec("INSERT INTO users(username) VALUES ('liisa')");
        assertFails("CHECK constraint failed",
                "INSERT INTO listed_items(owner_id, item_name, description, cost_per_day) "
                        + "VALUES (1, 'Drill', 'x', 0)");
    }

    @Test
    void negativeCostViolatesCheck() throws SQLException {
        exec("INSERT INTO users(username) VALUES ('liisa')");
        assertFails("CHECK constraint failed",
                "INSERT INTO listed_items(owner_id, item_name, description, cost_per_day) "
                        + "VALUES (1, 'Drill', 'x', -1)");
    }

    @Test
    void aCostOfOneIsAccepted() throws SQLException {
        exec("INSERT INTO users(username) VALUES ('liisa')");
        exec("INSERT INTO listed_items(owner_id, item_name, description, cost_per_day) "
                + "VALUES (1, 'Drill', 'x', 1)");

        assertEquals(1, count("SELECT count(*) FROM listed_items"));
    }

    @Test
    void itemWithUnknownOwnerViolatesForeignKey() {
        assertFails("FOREIGN KEY constraint failed",
                "INSERT INTO listed_items(owner_id, item_name, description, cost_per_day) "
                        + "VALUES (99, 'Drill', 'x', 3)");
    }

    // ---------- rentals ----------

    @Test
    void rentalForMissingItemViolatesForeignKey() throws SQLException {
        exec("INSERT INTO users(username) VALUES ('liisa')");
        assertFails("FOREIGN KEY constraint failed",
                "INSERT INTO rentals(item_id, renter_id, start_time, end_time) "
                        + "VALUES (99, 1, '2026-06-16 14:30', '2026-06-19 14:30')");
    }

    @Test
    void rentalWithUnknownRenterViolatesForeignKey() throws SQLException {
        seedOwnerRenterAndItem();
        assertFails("FOREIGN KEY constraint failed",
                "INSERT INTO rentals(item_id, renter_id, start_time, end_time) "
                        + "VALUES (1, 99, '2026-06-16 14:30', '2026-06-19 14:30')");
    }

    @Test
    void rentalWithBadStatusViolatesCheck() throws SQLException {
        seedOwnerRenterAndItem();
        assertFails("CHECK constraint failed",
                "INSERT INTO rentals(item_id, renter_id, start_time, end_time, status) "
                        + "VALUES (1, 2, '2026-06-16 14:30', '2026-06-19 14:30', 'lost')");
    }

    @Test
    void endBeforeStartViolatesCheck() throws SQLException {
        seedOwnerRenterAndItem();
        assertFails("CHECK constraint failed",
                "INSERT INTO rentals(item_id, renter_id, start_time, end_time) "
                        + "VALUES (1, 2, '2026-06-19 14:30', '2026-06-16 14:30')");
    }

    @Test
    void endEqualToStartViolatesCheck() throws SQLException {
        seedOwnerRenterAndItem();
        assertFails("CHECK constraint failed",
                "INSERT INTO rentals(item_id, renter_id, start_time, end_time) "
                        + "VALUES (1, 2, '2026-06-16 14:30', '2026-06-16 14:30')");
    }

    @Test
    void activeRentalWithReturnTimeViolatesCheck() throws SQLException {
        seedOwnerRenterAndItem();
        assertFails("CHECK constraint failed",
                "INSERT INTO rentals(item_id, renter_id, start_time, end_time, returned_at) "
                        + "VALUES (1, 2, '2026-06-16 14:30', '2026-06-19 14:30', '2026-06-17 10:00')");
    }

    @Test
    void closedRentalWithoutReturnTimeViolatesCheck() throws SQLException {
        seedOwnerRenterAndItem();
        assertFails("CHECK constraint failed",
                "INSERT INTO rentals(item_id, renter_id, start_time, end_time, status) "
                        + "VALUES (1, 2, '2026-06-16 14:30', '2026-06-19 14:30', 'closed')");
    }

    @Test
    void secondActiveRentalForSameItemViolatesUnique() throws SQLException {
        seedOwnerRenterAndItem();
        exec(ACTIVE_RENTAL);
        assertFails("UNIQUE constraint failed", ACTIVE_RENTAL);
    }

    @Test
    void newRentalIsAllowedOnceThePreviousOneIsClosed() throws SQLException {
        seedOwnerRenterAndItem();
        exec(ACTIVE_RENTAL);
        exec("UPDATE rentals SET status = 'closed', returned_at = '2026-06-18 09:00' "
                + "WHERE rental_id = 1");

        exec(ACTIVE_RENTAL);

        assertEquals(2, count("SELECT count(*) FROM rentals WHERE item_id = 1"));
    }
}