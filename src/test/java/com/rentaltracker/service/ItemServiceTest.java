package com.rentaltracker.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.enums.ItemStatus;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.ItemRepository;

class ItemServiceTest {

    @TempDir Path tempDir;
    Database db;
    ItemRepository itemRepository;
    ItemService service;

    @BeforeEach
    void setUp() throws SQLException {
        db = new Database(tempDir.resolve("test.db").toString());
        itemRepository = new ItemRepository(db.connection());
        service = new ItemService(itemRepository);

        // users: 1 and 2 (both can own items)
        exec("INSERT INTO users (username) VALUES ('liisa'), ('meelis')");
    }

    @AfterEach
    void tearDown() { db.close(); }

    // ---- helpers that talk to the database directly ----

    private void exec(String sql) throws SQLException {
        try (Statement st = db.connection().createStatement()) { st.execute(sql); }
    }

    private String query(String sql) throws SQLException {
        try (Statement st = db.connection().createStatement();
             var rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    // =================================================================
    // createItem
    // =================================================================

    @Test
    void createItemDefaultsToAvailableStatus() throws SQLException {
        ItemDomain item = service.createItem(1, "Ladder", "Tall", 5);

        assertEquals(ItemStatus.available, item.getStatus());
        assertEquals(1, item.getOwnerId());
        assertEquals("Ladder", item.getItemName());
        assertEquals("Tall", item.getDescription());
        assertEquals(5, item.getCostPerDay());
        assertNotNull(item.getCreatedAt());
        assertTrue(item.getItemId() > 0);
    }

    @Test
    void createItemPersistsToDatabase() throws SQLException {
        ItemDomain item = service.createItem(1, "Drill", "Powerful", 4);

        assertEquals("1", query("SELECT count(*) FROM listed_items"));
        assertTrue(itemRepository.findByItemId(item.getItemId()).isPresent());
    }

    @Test
    void createItemTrimsNameAndDescription() throws SQLException {
        ItemDomain item = service.createItem(1, "  Ladder  ", "  Tall  ", 5);

        assertEquals("Ladder", item.getItemName());
        assertEquals("Tall", item.getDescription());
    }

    @Test
    void createItemWithNullDescriptionBecomesEmptyString() throws SQLException {
        ItemDomain item = service.createItem(1, "Ladder", null, 5);

        assertEquals("", item.getDescription());
    }

    @Test
    void createItemWithExplicitStatusHonoursIt() throws SQLException {
        ItemDomain item = service.createItem(1, "Ladder", "x", 5, ItemStatus.rented);

        assertEquals(ItemStatus.rented, item.getStatus());
    }

    @Test
    void createItemWithNullStatusThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(1, "Ladder", "x", 5, null));
    }

    @Test
    void createItemWithNonPositiveOwnerThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(0, "Ladder", "x", 5));
        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(-1, "Ladder", "x", 5));
    }

    @Test
    void createItemWithNullOrBlankNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(1, null, "x", 5));
        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(1, "", "x", 5));
        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(1, "   ", "x", 5));
    }

    @Test
    void createItemWithNameLongerThan100CharsThrows() {
        String longName = "x".repeat(101);
        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(1, longName, "x", 5));
    }

    @Test
    void createItemWithNameExactly100CharsSucceeds() throws SQLException {
        String name = "x".repeat(100);
        ItemDomain item = service.createItem(1, name, "x", 5);

        assertEquals(name, item.getItemName());
    }

    @Test
    void createItemWithNegativeCostThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createItem(1, "Ladder", "x", -1));
    }

    @Test
    void createItemWithZeroCostIsAllowed() throws SQLException {
        ItemDomain item = service.createItem(1, "Ladder", "x", 0);

        assertEquals(0, item.getCostPerDay());
    }

    // =================================================================
    // getItemById / requireItemById
    // =================================================================

    @Test
    void getItemByIdReturnsTheStoredItem() throws SQLException {
        ItemDomain created = service.createItem(1, "Ladder", "x", 5);

        Optional<ItemDomain> found = service.getItemById(created.getItemId());

        assertTrue(found.isPresent());
        assertEquals(created.getItemId(), found.get().getItemId());
        assertEquals("Ladder", found.get().getItemName());
    }

    @Test
    void getItemByIdOnMissingReturnsEmpty() throws SQLException {
        assertTrue(service.getItemById(999).isEmpty());
    }

    @Test
    void getItemByIdWithNonPositiveIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.getItemById(0));
        assertThrows(IllegalArgumentException.class, () -> service.getItemById(-5));
    }

    @Test
    void requireItemByIdReturnsTheItem() throws SQLException {
        ItemDomain created = service.createItem(1, "Ladder", "x", 5);

        ItemDomain found = service.requireItemById(created.getItemId());

        assertEquals(created.getItemId(), found.getItemId());
    }

    @Test
    void requireItemByIdOnMissingThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.requireItemById(999));
    }

    // =================================================================
    // getAllItems
    // =================================================================

    @Test
    void getAllItemsReturnsEveryStoredItem() throws SQLException {
        service.createItem(1, "A", "x", 1);
        service.createItem(1, "B", "x", 1);
        service.createItem(2, "C", "x", 1);

        assertEquals(3, service.getAllItems().size());
    }

    @Test
    void getAllItemsIsEmptyWhenNothingExists() throws SQLException {
        assertTrue(service.getAllItems().isEmpty());
    }

    // =================================================================
    // getItemsByOwner
    // =================================================================

    @Test
    void getItemsByOwnerFiltersToThatOwner() throws SQLException {
        service.createItem(1, "A", "x", 1);
        service.createItem(1, "B", "x", 1);
        service.createItem(2, "C", "x", 1);

        List<ItemDomain> mine = service.getItemsByOwner(1);

        assertEquals(2, mine.size());
        assertTrue(mine.stream().allMatch(i -> i.getOwnerId() == 1));
    }

    @Test
    void getItemsByOwnerIsEmptyWhenOwnerHasNone() throws SQLException {
        service.createItem(1, "A", "x", 1);

        assertTrue(service.getItemsByOwner(2).isEmpty());
    }

    @Test
    void getItemsByOwnerWithNonPositiveIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.getItemsByOwner(0));
        assertThrows(IllegalArgumentException.class, () -> service.getItemsByOwner(-1));
    }

    // =================================================================
    // getItemsByStatus
    // =================================================================

    @Test
    void getItemsByStatusFiltersCorrectly() throws SQLException {
        service.createItem(1, "A", "x", 1, ItemStatus.available);
        service.createItem(1, "B", "x", 1, ItemStatus.rented);
        service.createItem(1, "C", "x", 1, ItemStatus.available);

        List<ItemDomain> available = service.getItemsByStatus(ItemStatus.available);

        assertEquals(2, available.size());
        assertTrue(available.stream().allMatch(i -> i.getStatus() == ItemStatus.available));
    }

    @Test
    void getItemsByStatusWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.getItemsByStatus(null));
    }

    @Test
    void getItemsByStatusReturnsEmptyWhenNothingMatches() throws SQLException {
        service.createItem(1, "A", "x", 1, ItemStatus.available);

        assertTrue(service.getItemsByStatus(ItemStatus.rented).isEmpty());
    }

    // =================================================================
    // updateItem
    // =================================================================

    @Test
    void updateItemChangesAllMutableFields() throws SQLException {
        ItemDomain created = service.createItem(1, "Old", "old desc", 5);

        ItemDomain updated = service.updateItem(
                created.getItemId(), "New", "new desc", 9, ItemStatus.rented);

        assertEquals("New", updated.getItemName());
        assertEquals("new desc", updated.getDescription());
        assertEquals(9, updated.getCostPerDay());
        assertEquals(ItemStatus.rented, updated.getStatus());
    }

    @Test
    void updateItemPersistsChanges() throws SQLException {
        ItemDomain created = service.createItem(1, "Old", "x", 5);

        service.updateItem(created.getItemId(), "New", "new desc", 9, ItemStatus.rented);

        ItemDomain reloaded = service.requireItemById(created.getItemId());
        assertEquals("New", reloaded.getItemName());
        assertEquals("new desc", reloaded.getDescription());
        assertEquals(9, reloaded.getCostPerDay());
        assertEquals(ItemStatus.rented, reloaded.getStatus());
    }

    @Test
    void updateItemPreservesOwnerAndCreatedAt() throws SQLException {
        ItemDomain created = service.createItem(1, "Old", "x", 5);
        LocalDateTime originalCreatedAt = created.getCreatedAt();

        ItemDomain updated = service.updateItem(
                created.getItemId(), "New", "x", 5, ItemStatus.available);

        assertEquals(1, updated.getOwnerId());
        assertEquals(originalCreatedAt, updated.getCreatedAt());
    }

    @Test
    void updateItemOnMissingIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateItem(999, "New", "x", 5, ItemStatus.available));
    }

    @Test
    void updateItemWithNonPositiveIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateItem(0, "New", "x", 5, ItemStatus.available));
    }

    @Test
    void updateItemWithBlankNameThrows() throws SQLException {
        ItemDomain created = service.createItem(1, "Old", "x", 5);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateItem(created.getItemId(), "   ", "x", 5, ItemStatus.available));
    }

    @Test
    void updateItemWithNegativeCostThrows() throws SQLException {
        ItemDomain created = service.createItem(1, "Old", "x", 5);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateItem(created.getItemId(), "New", "x", -1, ItemStatus.available));
    }

    @Test
    void updateItemWithNullStatusThrows() throws SQLException {
        ItemDomain created = service.createItem(1, "Old", "x", 5);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateItem(created.getItemId(), "New", "x", 5, null));
    }

    @Test
    void updateItemWithNullDescriptionBecomesEmptyString() throws SQLException {
        ItemDomain created = service.createItem(1, "Old", "has desc", 5);

        ItemDomain updated = service.updateItem(
                created.getItemId(), "New", null, 5, ItemStatus.available);

        assertEquals("", updated.getDescription());
    }

    // =================================================================
    // changeStatus
    // =================================================================

    @Test
    void changeStatusUpdatesStatus() throws SQLException {
        ItemDomain created = service.createItem(1, "Ladder", "x", 5, ItemStatus.available);

        ItemDomain updated = service.changeStatus(created.getItemId(), ItemStatus.rented);

        assertEquals(ItemStatus.rented, updated.getStatus());
    }

    @Test
    void changeStatusPersists() throws SQLException {
        ItemDomain created = service.createItem(1, "Ladder", "x", 5, ItemStatus.available);

        service.changeStatus(created.getItemId(), ItemStatus.rented);

        assertEquals(ItemStatus.rented,
                service.requireItemById(created.getItemId()).getStatus());
    }

    @Test
    void changeStatusWithNullThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(1, null));
    }

    @Test
    void changeStatusOnMissingIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(999, ItemStatus.rented));
    }

    // =================================================================
    // deleteItem
    // =================================================================

    @Test
    void deleteItemReturnsTrueAndRemovesRow() throws SQLException {
        ItemDomain created = service.createItem(1, "Doomed", "x", 5);

        assertTrue(service.deleteItem(created.getItemId()));
        assertTrue(service.getItemById(created.getItemId()).isEmpty());
    }

    @Test
    void deleteItemOnMissingReturnsFalse() throws SQLException {
        assertFalse(service.deleteItem(999));
    }

    @Test
    void deleteItemWithNonPositiveIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteItem(0));
        assertThrows(IllegalArgumentException.class, () -> service.deleteItem(-1));
    }

    @Test
    void deleteItemLeavesOtherItemsAlone() throws SQLException {
        ItemDomain kept   = service.createItem(1, "Kept",   "x", 5);
        ItemDomain doomed = service.createItem(1, "Doomed", "x", 5);

        service.deleteItem(doomed.getItemId());

        assertTrue(service.getItemById(kept.getItemId()).isPresent());
        assertEquals("1", query("SELECT count(*) FROM listed_items"));
    }

    // =================================================================
    // Integration sanity
    // =================================================================

    @Test
    void createThenUpdateThenDeleteRoundTrip() throws SQLException {
        ItemDomain created = service.createItem(1, "RoundTrip", "desc", 7);

        service.updateItem(created.getItemId(), "Renamed", "new", 9, ItemStatus.rented);
        assertEquals("Renamed", service.requireItemById(created.getItemId()).getItemName());

        assertTrue(service.deleteItem(created.getItemId()));
        assertTrue(service.getItemById(created.getItemId()).isEmpty());
    }
}