package com.rentaltracker.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rentaltracker.domain.RentalDomain;
import com.rentaltracker.domain.enums.RentalStatus;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.exception.ConstraintViolationException;
import com.rentaltracker.repository.exception.ConstraintViolationException.Type;
import com.rentaltracker.repository.exception.DatabaseConnectionException;
import com.rentaltracker.repository.exception.MappingException;
import com.rentaltracker.repository.exception.NotFoundException;
import com.rentaltracker.repository.exception.RepositoryException;

class RentalRepositoryTest {

    @TempDir Path tempDir;
    Database db;
    RentalRepository rentals;

    static final LocalDateTime START = LocalDateTime.of(2026, 6, 16, 14, 30);
    static final LocalDateTime END = LocalDateTime.of(2026, 6, 19, 14, 30);

    @BeforeEach
    void setUp() throws SQLException {
        db = new Database(tempDir.resolve("test.db").toString());
        rentals = new RentalRepository(db);

        // users: 1 owner, 2 renter, 3 a second owner
        exec("INSERT INTO users (username) VALUES ('liisa'), ('meelis'), ('marili')");
        // items 1-3 belong to user 1, item 4 belongs to user 3
        exec("INSERT INTO listed_items (owner_id, item_name, description, cost_per_day) VALUES "
           + "(1, 'Ladder', 'x', 5), (1, 'Drill', 'x', 4), (1, 'Tent', 'x', 8), (3, 'Saw', 'x', 6)");
    }

    @AfterEach
    void tearDown() { db.close(); }

    // ---- helpers that talk to the database directly ----

    private void exec(String sql) throws SQLException {
        try (Statement st = db.connection().createStatement()) { st.execute(sql); }
    }

    private String query(String sql) throws SQLException {
        try (Statement st = db.connection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    private RentalDomain newRental(long itemId, long renterId, LocalDateTime start, LocalDateTime end) {
        return new RentalDomain(0, itemId, renterId, start, end, null, RentalStatus.ACTIVE);
    }

    private long insertRental(long itemId, long renterId, LocalDateTime start, LocalDateTime end) {
        return rentals.insert(newRental(itemId, renterId, start, end));
    }

    // ---- insert ----

    @Test
    void insertStoresTheRowAndReturnsItsId() throws SQLException {
        long id = insertRental(1, 2, START, END);

        assertEquals(1, id);
        assertEquals("1", query("SELECT count(*) FROM rentals"));
        assertEquals("2026-06-16 14:30", query("SELECT start_time FROM rentals WHERE rental_id = " + id));
        assertEquals("2026-06-19 14:30", query("SELECT end_time FROM rentals WHERE rental_id = " + id));
    }

    @Test
    void insertedRentalIsActiveWithNoReturnTime() throws SQLException {
        long id = insertRental(1, 2, START, END);

        assertEquals("active", query("SELECT status FROM rentals WHERE rental_id = " + id));
        assertNull(query("SELECT returned_at FROM rentals WHERE rental_id = " + id));
    }

    @Test
    void insertForMissingItemViolatesForeignKey() {
        ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
                () -> insertRental(99, 2, START, END));
        assertEquals(Type.FOREIGN_KEY, e.getType());
    }

    @Test
    void insertForMissingRenterViolatesForeignKey() {
        ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
                () -> insertRental(1, 99, START, END));
        assertEquals(Type.FOREIGN_KEY, e.getType());
    }

    @Test
    void insertWithEndBeforeStartViolatesCheck() {
        ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
                () -> insertRental(1, 2, END, START));
        assertEquals(Type.CHECK, e.getType());
    }

    @Test
    void secondActiveRentalForSameItemViolatesUnique() {
        insertRental(1, 2, START, END);

        ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
                () -> insertRental(1, 3, START, END));
        assertEquals(Type.UNIQUE, e.getType());
    }

    // ---- findById ----

    @Test
    void findByIdReturnsTheStoredRental() {
        long id = insertRental(1, 2, START, END);

        RentalDomain found = rentals.findById(id);

        assertEquals(id, found.id());
        assertEquals(1, found.itemId());
        assertEquals(2, found.renterId());
        assertEquals(START, found.startTime());
        assertEquals(END, found.endTime());
        assertNull(found.returnTime());
        assertEquals(RentalStatus.ACTIVE, found.status());
    }

    @Test
    void findByIdOnMissingRentalThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> rentals.findById(999));
    }

    @Test
    void findByIdOnUnparseableRowThrowsMappingException() throws SQLException {
        // Plain SQL can store anything the CHECKs allow, so write a bad time directly
        exec("INSERT INTO rentals (item_id, renter_id, start_time, end_time) "
           + "VALUES (1, 2, 'garbage', 'zzzz')");

        assertThrows(MappingException.class, () -> rentals.findById(1));
    }

    // ---- findActiveByOwner ----

    @Test
    void findActiveByOwnerSortsByEndTimeAndSkipsClosedAndOtherOwners() throws SQLException {
        long later = insertRental(1, 2, START, LocalDateTime.of(2026, 6, 25, 10, 0));
        long sooner = insertRental(2, 2, START, LocalDateTime.of(2026, 6, 18, 10, 0));
        long closed = insertRental(3, 2, START, END);
        rentals.close(closed, LocalDateTime.of(2026, 6, 17, 9, 0));
        insertRental(4, 2, START, END);   // item 4 belongs to a different owner

        List<RentalDomain> result = rentals.findActiveByOwner(1);

        assertEquals(List.of(sooner, later), result.stream().map(RentalDomain::id).toList());
    }

    @Test
    void findActiveByOwnerIsEmptyWhenNothingIsOut() {
        assertTrue(rentals.findActiveByOwner(1).isEmpty());
    }

    // ---- findActiveByItem ----

    @Test
    void findActiveByItemReturnsTheActiveRental() {
        long id = insertRental(1, 2, START, END);

        Optional<RentalDomain> found = rentals.findActiveByItem(1);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().id());
    }

    @Test
    void findActiveByItemIsEmptyWhenTheItemIsNotOut() {
        assertTrue(rentals.findActiveByItem(1).isEmpty());
    }

    @Test
    void findActiveByItemIgnoresClosedRentals() {
        long id = insertRental(1, 2, START, END);
        rentals.close(id, LocalDateTime.of(2026, 6, 17, 9, 0));

        assertTrue(rentals.findActiveByItem(1).isEmpty());
    }

    // ---- close ----

    @Test
    void closeSetsStatusAndReturnTimeTogether() throws SQLException {
        long id = insertRental(1, 2, START, END);

        rentals.close(id, LocalDateTime.of(2026, 6, 18, 9, 5));

        assertEquals("closed", query("SELECT status FROM rentals WHERE rental_id = " + id));
        assertEquals("2026-06-18 09:05", query("SELECT returned_at FROM rentals WHERE rental_id = " + id));
    }

    @Test
    void closingTwiceThrowsNotFound() {
        long id = insertRental(1, 2, START, END);
        rentals.close(id, LocalDateTime.of(2026, 6, 18, 9, 5));

        assertThrows(NotFoundException.class,
                () -> rentals.close(id, LocalDateTime.of(2026, 6, 18, 9, 10)));
    }

    @Test
    void closingAMissingRentalThrowsNotFound() {
        assertThrows(NotFoundException.class,
                () -> rentals.close(999, LocalDateTime.of(2026, 6, 18, 9, 5)));
    }

    @Test
    void anItemCanBeRentedAgainOnceTheLastRentalIsClosed() throws SQLException {
        long first = insertRental(1, 2, START, END);
        rentals.close(first, LocalDateTime.of(2026, 6, 18, 9, 5));

        insertRental(1, 3, START, END);

        assertEquals("2", query("SELECT count(*) FROM rentals WHERE item_id = 1"));
    }

    @Test
    void insertOnClosedDatabaseThrowsRepositoryException() {
        db.close();
        assertThrows(RepositoryException.class, () -> insertRental(1, 2, START, END));
    }

    @Test
    void badPathThrowsDatabaseConnectionException() {
        assertThrows(DatabaseConnectionException.class,
                () -> new Database("/nonexistent/dir/x.db"));
    }

    @Test
    void foreignKeysAreEnabled(@TempDir Path dir) throws SQLException {
        try (Database db = new Database(dir.resolve("t.db").toString());
            Statement st = db.connection().createStatement();
            ResultSet rs = st.executeQuery("PRAGMA foreign_keys")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }
}