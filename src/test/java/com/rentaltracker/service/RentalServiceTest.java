package com.rentaltracker.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.RentalDomain;
import com.rentaltracker.domain.enums.ItemStatus;
import com.rentaltracker.domain.enums.RentalStatus;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.ItemRepository;
import com.rentaltracker.repository.RentalRepository;
import com.rentaltracker.repository.SqliteTransactor;
import com.rentaltracker.repository.exception.NotFoundException;
import com.rentaltracker.repository.exception.RepositoryException;
import com.rentaltracker.service.exception.BusinessRuleException;

class RentalServiceTest {

    // "Now" for every test: 16 June 2026, 14:30:45 (seconds get truncated by the service)
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-16T14:30:45Z"), ZoneId.of("UTC"));
    static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 16, 14, 30);

    static final LocalDateTime START = LocalDateTime.of(2026, 6, 10, 9, 0);
    static final LocalDateTime END = LocalDateTime.of(2026, 6, 13, 9, 0);

    @TempDir Path tempDir;
    Database db;
    ItemRepository items;
    RentalRepository rentals;
    RentalService service;

    @BeforeEach
    void setUp() throws SQLException {
        db = new Database(tempDir.resolve("test.db").toString());
        items = new ItemRepository(db.connection());
        rentals = new RentalRepository(db);
        service = new RentalService(items, rentals, new SqliteTransactor(db), CLOCK);

        // user 1 = owner, 2 = renter, 3 = second owner
        exec("INSERT INTO users (username) VALUES ('liisa'), ('meelis'), ('marili')");
    }

    @AfterEach
    void tearDown() { db.close(); }

    // ---------- helpers that talk to the database directly ----------

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

    private String itemStatus(long itemId) throws SQLException {
        return query("SELECT status FROM listed_items WHERE item_id = " + itemId);
    }

    private String rentalStatus(long rentalId) throws SQLException {
        return query("SELECT status FROM rentals WHERE rental_id = " + rentalId);
    }

    private String returnedAt(long rentalId) throws SQLException {
        return query("SELECT returned_at FROM rentals WHERE rental_id = " + rentalId);
    }

    private String rentalCount() throws SQLException {
        return query("SELECT count(*) FROM rentals");
    }

    /** Inserts an item with the given status and returns its id. */
    private long addItem(int ownerId, String name, ItemStatus status) throws SQLException {
        exec("INSERT INTO listed_items (owner_id, item_name, description, cost_per_day, status) "
           + "VALUES (" + ownerId + ", '" + name + "', 'x', 5, '" + status.name() + "')");
        return Long.parseLong(query("SELECT last_insert_rowid()"));
    }

    private long addActiveRental(long itemId, LocalDateTime end) {
        return rentals.insert(new RentalDomain(0, itemId, 2, START, end, null, RentalStatus.ACTIVE));
    }

    // ---------- confirmReturn ----------

    @Test
    void confirmReturnClosesTheRentalAndFreesTheItem() throws SQLException {
        long itemId = addItem(1, "Ladder", ItemStatus.rented);
        long rentalId = addActiveRental(itemId, END);

        ItemDomain result = service.confirmReturn(rentalId);

        assertEquals("closed", rentalStatus(rentalId));
        assertEquals("2026-06-16 14:30", returnedAt(rentalId));
        assertEquals("available", itemStatus(itemId));
        assertEquals(ItemStatus.available, result.getStatus());
    }

    @Test
    void confirmReturnLeavesAnItemUnlistedIfItWasDelistedWhileOut() throws SQLException {
        long itemId = addItem(1, "Ladder", ItemStatus.unlisted);
        long rentalId = addActiveRental(itemId, END);

        service.confirmReturn(rentalId);

        assertEquals("closed", rentalStatus(rentalId));
        assertEquals("unlisted", itemStatus(itemId));
    }

    @Test
    void confirmReturnKeepsTheRentalRecord() throws SQLException {
        long itemId = addItem(1, "Ladder", ItemStatus.rented);
        long rentalId = addActiveRental(itemId, END);

        service.confirmReturn(rentalId);

        assertEquals("1", rentalCount());
    }

    @Test
    void confirmReturnTwiceIsRejectedAndChangesNothing() throws SQLException {
        long itemId = addItem(1, "Ladder", ItemStatus.rented);
        long rentalId = addActiveRental(itemId, END);
        service.confirmReturn(rentalId);

        assertThrows(BusinessRuleException.class, () -> service.confirmReturn(rentalId));

        assertEquals("closed", rentalStatus(rentalId));
        assertEquals("available", itemStatus(itemId));
    }

    @Test
    void confirmReturnOnAMissingRentalThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> service.confirmReturn(999));
    }

    @Test
    void confirmReturnRollsBackTheRentalIfTheItemUpdateFails() throws SQLException {
        long itemId = addItem(1, "Ladder", ItemStatus.rented);
        long rentalId = addActiveRental(itemId, END);
        // make every item update fail, after the rental has already been closed
        exec("CREATE TRIGGER block_item_updates BEFORE UPDATE ON listed_items "
           + "BEGIN SELECT RAISE(ABORT, 'blocked'); END");

        assertThrows(RepositoryException.class, () -> service.confirmReturn(rentalId));

        assertEquals("active", rentalStatus(rentalId));
        assertNull(returnedAt(rentalId));
        assertEquals("rented", itemStatus(itemId));
    }

    // ---------- recordRental: rejections ----------

    @Test
    void recordRentalOnARentedItemIsRejectedAndChangesNothing() throws SQLException {
        long itemId = addItem(1, "Ladder", ItemStatus.rented);

        assertThrows(BusinessRuleException.class, () -> service.recordRental((int) itemId, "meelis", 3));

        assertEquals("0", rentalCount());
        assertEquals("rented", itemStatus(itemId));
    }

    @Test
    void recordRentalOnAnUnlistedItemIsRejectedAndChangesNothing() throws SQLException {
        long itemId = addItem(1, "Ladder", ItemStatus.unlisted);

        assertThrows(BusinessRuleException.class, () -> service.recordRental((int) itemId, "meelis", 3));

        assertEquals("0", rentalCount());
        assertEquals("unlisted", itemStatus(itemId));
    }

    @Test
    void recordRentalOnAMissingItemThrowsNotFound() throws SQLException {
        assertThrows(NotFoundException.class, () -> service.recordRental(999, "meelis", 3));

        assertEquals("0", rentalCount());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void recordRentalRejectsABlankRenterName(String name) throws SQLException {
        long itemId = addItem(1, "Ladder", ItemStatus.available);

        assertThrows(IllegalArgumentException.class, () -> service.recordRental((int) itemId, name, 3));

        assertEquals("0", rentalCount());
        assertEquals("available", itemStatus(itemId));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 366})
    void recordRentalRejectsAnInvalidNumberOfDays(int days) throws SQLException {
        long itemId = addItem(1, "Ladder", ItemStatus.available);

        assertThrows(IllegalArgumentException.class, () -> service.recordRental((int) itemId, "meelis", days));

        assertEquals("0", rentalCount());
        assertEquals("available", itemStatus(itemId));
    }

    // ---------- getRentableItems ----------

    @Test
    void getRentableItemsReturnsOnlyTheOwnersAvailableItems() throws SQLException {
        addItem(1, "Ladder", ItemStatus.available);
        addItem(1, "Drill", ItemStatus.rented);
        addItem(1, "Tent", ItemStatus.unlisted);
        addItem(1, "Sander", ItemStatus.available);
        addItem(3, "Saw", ItemStatus.available);   // someone else's

        List<String> names = service.getRentableItems(1).stream()
                .map(ItemDomain::getItemName).toList();

        assertEquals(List.of("Ladder", "Sander"), names);
    }

    @Test
    void getRentableItemsIsEmptyWhenNothingIsAvailable() throws SQLException {
        addItem(1, "Drill", ItemStatus.rented);

        assertTrue(service.getRentableItems(1).isEmpty());
    }

    // ---------- getActiveRentals ----------

    @Test
    void getActiveRentalsSortsByDueDateAndSkipsClosedAndOtherOwners() throws SQLException {
        long a = addItem(1, "Ladder", ItemStatus.rented);
        long b = addItem(1, "Drill", ItemStatus.rented);
        long c = addItem(1, "Tent", ItemStatus.available);
        long other = addItem(3, "Saw", ItemStatus.rented);

        long later = addActiveRental(a, LocalDateTime.of(2026, 6, 25, 10, 0));
        long sooner = addActiveRental(b, LocalDateTime.of(2026, 6, 18, 10, 0));
        long closed = addActiveRental(c, END);
        rentals.close(closed, LocalDateTime.of(2026, 6, 12, 9, 0));
        addActiveRental(other, END);

        List<Long> ids = service.getActiveRentals(1).stream().map(RentalDomain::id).toList();

        assertEquals(List.of(sooner, later), ids);
    }

    @Test
    void getActiveRentalsIsEmptyWhenNothingIsOut() {
        assertTrue(service.getActiveRentals(1).isEmpty());
    }
}