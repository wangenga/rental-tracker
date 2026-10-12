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

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.enums.ItemStatus;
import com.rentaltracker.infrastructure.Database;

class ListedItemRepositoryTest {

    @TempDir Path tempDir;
    Database db;
    ItemRepository items;

    @BeforeEach
    void setUp() throws SQLException {
        db = new Database(tempDir.resolve("test.db").toString());
        items = new ItemRepository(db.connection());

        // users: 1 and 2 (both potential owners)
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
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    private ItemDomain newItem(int ownerId, String name, String desc, int costPerDay) {
        return new ItemDomain(0, ownerId, name, desc, costPerDay, ItemStatus.available, null);
    }

    private ItemDomain saveItem(int ownerId, String name, String desc, int costPerDay)
            throws SQLException {
        ItemDomain item = newItem(ownerId, name, desc, costPerDay);
        items.save(item);
        return item;
    }

    // ---- save ----

    @Test
    void saveStoresTheRowAndSetsTheId() throws SQLException {
        ItemDomain item = saveItem(1, "Ladder", "Tall", 5);

        assertTrue(item.getItemId() > 0);
        assertEquals("1", query("SELECT count(*) FROM listed_items"));
        assertEquals("1",     query("SELECT owner_id FROM listed_items WHERE item_id = " + item.getItemId()));
        assertEquals("Ladder", query("SELECT item_name FROM listed_items WHERE item_id = " + item.getItemId()));
        assertEquals("Tall",   query("SELECT description FROM listed_items WHERE item_id = " + item.getItemId()));
        assertEquals("5",     query("SELECT cost_per_day FROM listed_items WHERE item_id = " + item.getItemId()));
    }

    @Test
    void saveSetsCreatedAtOnTheDomainObject() throws SQLException {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        ItemDomain item = saveItem(1, "Drill", "Powerful", 4);

        assertNotNull(item.getCreatedAt());
        assertTrue(item.getCreatedAt().isAfter(before));
        assertNotNull(query("SELECT created_at FROM listed_items WHERE item_id = " + item.getItemId()));
    }

    @Test
    void saveAssignsUniqueIdsToEachItem() throws SQLException {
        ItemDomain a = saveItem(1, "A", "x", 1);
        ItemDomain b = saveItem(1, "B", "x", 1);

        assertNotEquals(a.getItemId(), b.getItemId());
    }

    @Test
    void saveForMissingOwnerViolatesForeignKey() {
        ItemDomain item = newItem(99, "Ghost", "x", 5);

        assertThrows(SQLException.class, () -> items.save(item));
    }

    // ---- findByItemId ----

    @Test
    void findByItemIdReturnsTheStoredItem() throws SQLException {
        ItemDomain item = saveItem(1, "Drill", "Powerful", 4);

        Optional<ItemDomain> found = items.findByItemId(item.getItemId());

        assertTrue(found.isPresent());
        ItemDomain loaded = found.get();
        assertEquals(item.getItemId(), loaded.getItemId());
        assertEquals(1, loaded.getOwnerId());
        assertEquals("Drill", loaded.getItemName());
        assertEquals("Powerful", loaded.getDescription());
        assertEquals(4, loaded.getCostPerDay());
        assertEquals(ItemStatus.available, loaded.getStatus());
        assertNotNull(loaded.getCreatedAt());
    }

    @Test
    void findByItemIdIsEmptyForMissingItem() throws SQLException {
        assertTrue(items.findByItemId(999).isEmpty());
    }

    // ---- findAll ----

    @Test
    void findAllReturnsEveryStoredItem() throws SQLException {
        saveItem(1, "A", "x", 1);
        saveItem(1, "B", "x", 2);
        saveItem(2, "C", "x", 3);

        List<ItemDomain> all = items.findAll();

        assertEquals(3, all.size());
    }

    @Test
    void findAllIsEmptyWhenNothingIsStored() throws SQLException {
        assertTrue(items.findAll().isEmpty());
    }

    // ---- update ----

    @Test
    void updateChangesAllMutableFields() throws SQLException {
        ItemDomain item = saveItem(1, "Old", "old desc", 5);

        item.setItemName("New");
        item.setDescription("new desc");
        item.setCostPerDay(9);
        item.setOwnerId(2);
        items.update(item);

        assertEquals("New",      query("SELECT item_name FROM listed_items WHERE item_id = " + item.getItemId()));
        assertEquals("new desc", query("SELECT description FROM listed_items WHERE item_id = " + item.getItemId()));
        assertEquals("9",       query("SELECT cost_per_day FROM listed_items WHERE item_id = " + item.getItemId()));
        assertEquals("2",       query("SELECT owner_id FROM listed_items WHERE item_id = " + item.getItemId()));
    }

    @Test
    void updateChangesStatus() throws SQLException {
        ItemDomain item = saveItem(1, "Drill", "x", 4);

        item.setStatus(ItemStatus.rented);
        items.update(item);

        assertEquals(ItemStatus.rented,
                items.findByItemId(item.getItemId()).orElseThrow().getStatus());
    }

    @Test
    void updateOnMissingIdIsANoOp() throws SQLException {
        ItemDomain ghost = newItem(1, "Ghost", "x", 1); // itemId == 0

        items.update(ghost);

        assertEquals("0", query("SELECT count(*) FROM listed_items"));
    }

    // ---- deleteById ----

    @Test
    void deleteByIdRemovesTheRow() throws SQLException {
        ItemDomain item = saveItem(1, "Doomed", "x", 1);

        items.deleteById(item.getItemId());

        assertEquals("0", query("SELECT count(*) FROM listed_items"));
        assertTrue(items.findByItemId(item.getItemId()).isEmpty());
    }

    @Test
    void deleteByIdLeavesOtherRowsAlone() throws SQLException {
        ItemDomain kept   = saveItem(1, "Kept",   "x", 1);
        ItemDomain doomed = saveItem(1, "Doomed", "x", 1);

        items.deleteById(doomed.getItemId());

        assertEquals("1", query("SELECT count(*) FROM listed_items"));
        assertTrue(items.findByItemId(kept.getItemId()).isPresent());
    }

    @Test
    void deleteByIdOnMissingIdIsANoOp() throws SQLException {
        saveItem(1, "Kept", "x", 1);

        items.deleteById(999);

        assertEquals("1", query("SELECT count(*) FROM listed_items"));
    }

    // ---- round-trip sanity ----

    @Test
    void savedItemCanBeReadBackThroughFindByItemId() throws SQLException {
        ItemDomain item = saveItem(1, "RoundTrip", "desc", 7);

        ItemDomain loaded = items.findByItemId(item.getItemId()).orElseThrow();

        assertEquals(item.getItemName(),  loaded.getItemName());
        assertEquals(item.getOwnerId(),   loaded.getOwnerId());
        assertEquals(item.getCostPerDay(), loaded.getCostPerDay());
    }
}