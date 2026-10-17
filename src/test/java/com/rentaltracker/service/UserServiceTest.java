package com.rentaltracker.service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.rentaltracker.domain.User;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.UserRepository;
import com.rentaltracker.repository.exception.NotFoundException;
import com.rentaltracker.service.exception.BusinessRuleException;

class UserServiceTest {

    @TempDir Path tempDir;
    String dbPath;
    Database db;
    UserService service;

    @BeforeEach
    void setUp() {
        dbPath = tempDir.resolve("test.db").toString();
        db = new Database(dbPath);
        service = new UserService(new UserRepository(db));
    }

    @AfterEach
    void tearDown() { db.close(); }

    // ---------- helpers that talk to the database directly ----------

    private void exec(String sql) throws SQLException {
        try (Statement st = db.connection().createStatement()) { st.execute(sql); }
    }

    private String query(Database database, String sql) throws SQLException {
        try (Statement st = database.connection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    private String userCount() throws SQLException {
        return query(db, "SELECT count(*) FROM users");
    }

    // ---------- the owner (first launch) ----------

    @Test
    void firstLaunchHasNoOwner() {
        assertTrue(service.findOwner().isEmpty());
    }

    @Test
    void createOwnerStoresANormalisedUserWithNoPassword() throws SQLException {
        User owner = service.createOwner("  Liisa ");

        assertEquals("liisa", owner.getUsername());
        assertEquals("1", userCount());
        assertNull(query(db, "SELECT password FROM users WHERE id = " + owner.getId()));
    }

    @Test
    void aSecondRunLoadsTheSameAccountInsteadOfCreatingANewOne() throws SQLException {
        User created = service.createOwner("liisa");
        db.close();   // the app exits

        try (Database again = new Database(dbPath)) {   // the app starts again
            UserService secondRun = new UserService(new UserRepository(again));

            User loaded = secondRun.findOwner().orElseThrow();

            assertEquals(created.getId(), loaded.getId());
            assertEquals("1", query(again, "SELECT count(*) FROM users"));
        }
    }

    @Test
    void createOwnerIsRefusedOnceAnyUserExists() throws SQLException {
        service.createOwner("liisa");

        assertThrows(BusinessRuleException.class, () -> service.createOwner("meelis"));

        assertEquals("1", userCount());
    }

    @Test
    void createOwnerRejectsAnInvalidNameAndStoresNothing() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> service.createOwner("ab"));

        assertEquals("0", userCount());
    }

    @Test
    void theOwnerIsTheLowestIdUser() throws SQLException {
        exec("INSERT INTO users (id, username) VALUES (5, 'zed'), (2, 'amy')");

        assertEquals("amy", service.findOwner().orElseThrow().getUsername());
    }

    // ---------- createUser ----------

    @Test
    void createUserStoresANormalisedUserWithNoPassword() throws SQLException {
        User user = service.createUser("  Meelis ");

        assertEquals("meelis", user.getUsername());
        assertNull(query(db, "SELECT password FROM users WHERE id = " + user.getId()));
        assertEquals("1", userCount());
    }

    @Test
    void createUserRejectsATakenUsernameEvenWithDifferentCapitalisation() throws SQLException {
        service.createUser("meelis");

        assertThrows(IllegalArgumentException.class, () -> service.createUser("MEELIS"));

        assertEquals("1", userCount());
    }

    // ---------- findOrCreate ----------

    @Test
    void findOrCreateCreatesAnUnknownUser() throws SQLException {
        User user = service.findOrCreate("newrenter");

        assertEquals("newrenter", user.getUsername());
        assertEquals("1", userCount());
    }

    @Test
    void findOrCreateReusesAnExistingUserIgnoringCaseAndSpaces() throws SQLException {
        User first = service.createUser("meelis");

        User again = service.findOrCreate("  Meelis ");

        assertEquals(first.getId(), again.getId());
        assertEquals("1", userCount());
    }

    // ---------- reads ----------

    @Test
    void getUserReturnsTheStoredUser() {
        User created = service.createUser("meelis");

        assertEquals("meelis", service.getUser(created.getId()).getUsername());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void getUserRejectsANonPositiveId(long id) {
        assertThrows(IllegalArgumentException.class, () -> service.getUser(id));
    }

    @Test
    void getUserOnAMissingIdThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getUser(999));
    }

    @Test
    void getUserByUsernameFindsAUserFromAnyCapitalisation() {
        User created = service.createUser("meelis");

        assertEquals(created.getId(), service.getUserByUsername(" MEELIS ").getId());
    }

    @Test
    void getUserByUsernameOnAnUnknownNameThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getUserByUsername("nobody"));
    }

    @Test
    void getAllUsersReturnsThemInIdOrder() throws SQLException {
        exec("INSERT INTO users (id, username) VALUES (5, 'zed'), (2, 'amy')");

        List<String> names = service.getAllUsers().stream().map(User::getUsername).toList();

        assertEquals(List.of("amy", "zed"), names);
    }

    // ---------- username validation ----------

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "ab", "bad name", "bad-name", "a!b"})
    void invalidUsernamesAreRejectedAndNothingIsStored(String name) throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> service.createUser(name));

        assertEquals("0", userCount());
    }

    @Test
    void aUsernameOverFiftyCharactersIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.createUser("a".repeat(51)));
    }

    @Test
    void aUsernameOfExactlyFiftyCharactersIsAccepted() {
        assertEquals(50, service.createUser("a".repeat(50)).getUsername().length());
    }

    @Test
    void aNullUsernameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.createUser(null));
    }
}