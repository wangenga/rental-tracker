package com.rentaltracker.transport.screens;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.enums.ItemStatus;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.ItemRepository;
import com.rentaltracker.repository.RentalRepository;
import com.rentaltracker.repository.SqliteTransactor;
import com.rentaltracker.repository.UserRepository;
import com.rentaltracker.service.ItemService;
import com.rentaltracker.service.UserService;
import com.rentaltracker.transport.Prompter;
import com.rentaltracker.transport.exception.InputClosedException;

/** Drives the screen with scripted console input against a fresh SQLite file per test. */
class ListItemScreenTest {

    static final int OWNER_ID = 1;
    static final int OTHER_OWNER_ID = 2;

    @TempDir Path tempDir;
    Database db;
    ItemService items;
    ByteArrayOutputStream out;

    @BeforeEach
    void setUp() throws SQLException {
        db = new Database(tempDir.resolve("test.db").toString());
        items = new ItemService(new ItemRepository(db.connection()), new RentalRepository(db),
                new UserService(new UserRepository(db)), new SqliteTransactor(db));
        out = new ByteArrayOutputStream();
        // 1 = owner, 2 = another owner
        exec("INSERT INTO users (username) VALUES ('liisa'), ('marili')");
    }

    @AfterEach
    void tearDown() { db.close(); }

    // ---------- helpers ----------

    private void exec(String sql) throws SQLException {
        try (Statement st = db.connection().createStatement()) { st.execute(sql); }
    }

    /** Builds a screen whose console input is the given lines. */
    private ListItemScreen screenWithInput(String... lines) {
        String input = lines.length == 0 ? "" : String.join("\n", lines) + "\n";
        Prompter prompter = new Prompter(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8));
        return new ListItemScreen(prompter, items);
    }

    private String output() { return out.toString(StandardCharsets.UTF_8); }

    private ItemDomain onlyItemOf(int ownerId) {
        List<ItemDomain> owned = items.getItemsByOwner(ownerId);
        assertEquals(1, owned.size(), "expected exactly one listed item");
        return owned.get(0);
    }

    private static int countOf(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) count++;
        return count;
    }

    // ---------- happy path ----------

    @Test
    void listsItemAsAvailableAndConfirms() {
        screenWithInput("Drill", "Cordless", "15").run(OWNER_ID);

        ItemDomain item = onlyItemOf(OWNER_ID);
        assertEquals("Drill", item.getItemName());
        assertEquals("Cordless", item.getDescription());
        assertEquals(15, item.getCostPerDay());
        assertEquals(ItemStatus.available, item.getStatus());
        assertTrue(output().contains("=== List an item ==="));
        assertTrue(output().contains("Listed Drill at 15 per day."));
    }

    @Test
    void itemBelongsToTheOwnerItWasGiven() {
        screenWithInput("Ladder", "", "5").run(OTHER_OWNER_ID);

        assertEquals("Ladder", onlyItemOf(OTHER_OWNER_ID).getItemName());
        assertTrue(items.getItemsByOwner(OWNER_ID).isEmpty());
    }

    @Test
    void emptyDescriptionIsAllowed() {
        screenWithInput("Ladder", "", "5").run(OWNER_ID);

        assertEquals("", onlyItemOf(OWNER_ID).getDescription());
    }

    @Test
    void answersAreTrimmed() {
        screenWithInput("  Tent  ", "  Four person ", " 20 ").run(OWNER_ID);

        ItemDomain item = onlyItemOf(OWNER_ID);
        assertEquals("Tent", item.getItemName());
        assertEquals("Four person", item.getDescription());
        assertEquals(20, item.getCostPerDay());
    }

    // ---------- re-prompting ----------

    @Test
    void blankNameIsAskedAgain() {
        screenWithInput("", "   ", "Drill", "", "15").run(OWNER_ID);

        assertEquals("Drill", onlyItemOf(OWNER_ID).getItemName());
        assertEquals(2, countOf(output(), "Please enter a value."));
    }

    @Test
    void nonNumericCostIsAskedAgain() {
        screenWithInput("Drill", "", "abc", "1.5", "15").run(OWNER_ID);

        assertEquals(15, onlyItemOf(OWNER_ID).getCostPerDay());
        assertEquals(2, countOf(output(), "Please enter a whole number."));
    }

    // ---------- failures go up to MainMenu ----------

    @Test
    void invalidCostPropagatesAndSavesNothing() {
        ListItemScreen screen = screenWithInput("Drill", "", "0");

        assertThrows(IllegalArgumentException.class, () -> screen.run(OWNER_ID));

        assertTrue(items.getItemsByOwner(OWNER_ID).isEmpty());
        assertFalse(output().contains("Listed"));
    }

    @Test
    void inputClosedMidwaySavesNothing() {
        ListItemScreen screen = screenWithInput("Drill");

        assertThrows(InputClosedException.class, () -> screen.run(OWNER_ID));

        assertTrue(items.getItemsByOwner(OWNER_ID).isEmpty());
    }

    @Test
    void inputClosedBeforeAnythingSavesNothing() {
        ListItemScreen screen = screenWithInput();

        assertThrows(InputClosedException.class, () -> screen.run(OWNER_ID));

        assertTrue(items.getItemsByOwner(OWNER_ID).isEmpty());
    }
}
