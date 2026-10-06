package com.rentaltracker.repository;

import com.rentaltracker.domain.User;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.exception.NotFoundException;
import com.rentaltracker.repository.exception.RepositoryException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {

    private Path databasePath;
    private Database database;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws Exception {
        databasePath = Files.createTempFile(
                "rental-tracker-test-",
                ".db"
        );

        database = new Database(databasePath.toString());
        userRepository = new UserRepository(database);
    }

    @AfterEach
    void tearDown() throws Exception {
        database.close();
        Files.deleteIfExists(databasePath);
    }

    @Test
    void findById_returnsExistingUser() {

        User savedUser = userRepository.save(
                new User(
                        0,
                        "caleb",
                        "password123",
                        null
                )
        );

        User foundUser =
                userRepository.findById(savedUser.getId());

        assertEquals(savedUser.getId(), foundUser.getId());
        assertEquals("caleb", foundUser.getUsername());
        assertEquals("password123", foundUser.getPassword());
        assertNotNull(foundUser.getCreatedAt());
    }

    @Test
    void findById_throwsNotFoundExceptionWhenUserDoesNotExist() {

        assertThrows(
                NotFoundException.class,
                () -> userRepository.findById(999999)
        );
    }

    @Test
    void findById_throwsNotFoundExceptionForZeroId() {

        assertThrows(
                NotFoundException.class,
                () -> userRepository.findById(0)
        );
    }

    @Test
    void findById_throwsNotFoundExceptionForNegativeId() {

        assertThrows(
                NotFoundException.class,
                () -> userRepository.findById(-1)
        );
    }

    @Test
    void findByUsername_returnsExistingUser() {

        userRepository.save(
                new User(
                        0,
                        "alice",
                        "secret123",
                        null
                )
        );

        User foundUser =
                userRepository.findByUsername("alice");

        assertTrue(foundUser.getId() > 0);
        assertEquals("alice", foundUser.getUsername());
        assertEquals("secret123", foundUser.getPassword());
        assertNotNull(foundUser.getCreatedAt());
    }

    @Test
    void findByUsername_throwsNotFoundExceptionWhenUserDoesNotExist() {

        assertThrows(
                NotFoundException.class,
                () -> userRepository.findByUsername("does-not-exist")
        );
    }

    @Test
    void findAll_returnsAllUsersOrderedById() {

        User firstUser = userRepository.save(
                new User(
                        0,
                        "alice",
                        "secret1",
                        null
                )
        );

        User secondUser = userRepository.save(
                new User(
                        0,
                        "bob",
                        "secret2",
                        null
                )
        );

        List<User> users = userRepository.findAll();

        assertEquals(2, users.size());

        assertEquals(
                firstUser.getId(),
                users.get(0).getId()
        );

        assertEquals(
                secondUser.getId(),
                users.get(1).getId()
        );

        assertEquals("alice", users.get(0).getUsername());
        assertEquals("bob", users.get(1).getUsername());
    }

    @Test
    void findAll_returnsEmptyListWhenNoUsersExist() {

        List<User> users = userRepository.findAll();

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void save_createsUserAndReturnsPersistedUser() {

        User user = new User(
                0,
                "caleb",
                "password123",
                null
        );

        User savedUser = userRepository.save(user);

        assertTrue(savedUser.getId() > 0);
        assertEquals("caleb", savedUser.getUsername());
        assertEquals("password123", savedUser.getPassword());
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    void save_generatesDifferentIdsForDifferentUsers() {

        User firstUser = userRepository.save(
                new User(
                        0,
                        "alice",
                        "secret1",
                        null
                )
        );

        User secondUser = userRepository.save(
                new User(
                        0,
                        "bob",
                        "secret2",
                        null
                )
        );

        assertNotEquals(
                firstUser.getId(),
                secondUser.getId()
        );
    }

    @Test
    void save_throwsRepositoryExceptionWhenUsernameAlreadyExists() {

        userRepository.save(
                new User(
                        0,
                        "caleb",
                        "password123",
                        null
                )
        );

        assertThrows(
                RepositoryException.class,
                () -> userRepository.save(
                        new User(
                                0,
                                "caleb",
                                "differentPassword",
                                null
                        )
                )
        );
    }

    @Test
    void save_throwsRepositoryExceptionWhenUsernameIsNull() {

        assertThrows(
                RepositoryException.class,
                () -> userRepository.save(
                        new User(
                                0,
                                null,
                                "password123",
                                null
                        )
                )
        );
    }

    @Test
    void save_throwsRepositoryExceptionWhenUsernameIsBlank() {

        assertThrows(
                RepositoryException.class,
                () -> userRepository.save(
                        new User(
                                0,
                                "   ",
                                "password123",
                                null
                        )
                )
        );
    }

    @Test
    void save_throwsRepositoryExceptionWhenUsernameIsEmpty() {

        assertThrows(
                RepositoryException.class,
                () -> userRepository.save(
                        new User(
                                0,
                                "",
                                "password123",
                                null
                        )
                )
        );
    }

    @Test
    void save_throwsRepositoryExceptionWhenPasswordIsNull() {

        assertThrows(
                RepositoryException.class,
                () -> userRepository.save(
                        new User(
                                0,
                                "caleb",
                                null,
                                null
                        )
                )
        );
    }

    @Test
    void save_allowsBlankPasswordIfSchemaAllowsIt() {

        User savedUser = userRepository.save(
                new User(
                        0,
                        "caleb",
                        "",
                        null
                )
        );

        assertTrue(savedUser.getId() > 0);
        assertEquals("", savedUser.getPassword());
    }

    @Test
    void save_persistsCreatedAtInExpectedFormat() {

        User savedUser = userRepository.save(
                new User(
                        0,
                        "caleb",
                        "password123",
                        null
                )
        );

        assertNotNull(savedUser.getCreatedAt());

        assertDoesNotThrow(() ->
                LocalDateTime.parse(
                        savedUser.getCreatedAt(),
                        DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd HH:mm:ss"
                        )
                )
        );
    }

    @Test
    void findById_returnsCorrectMappedUser() {

        User savedUser = userRepository.save(
                new User(
                        0,
                        "caleb",
                        "password123",
                        null
                )
        );

        User foundUser =
                userRepository.findById(savedUser.getId());

        assertAll(
                () -> assertEquals(
                        savedUser.getId(),
                        foundUser.getId()
                ),
                () -> assertEquals(
                        savedUser.getUsername(),
                        foundUser.getUsername()
                ),
                () -> assertEquals(
                        savedUser.getPassword(),
                        foundUser.getPassword()
                ),
                () -> assertEquals(
                        savedUser.getCreatedAt(),
                        foundUser.getCreatedAt()
                )
        );
    }

    @Test
    void findById_throwsRepositoryExceptionWhenUsersTableDoesNotExist()
            throws Exception {

        database.connection().createStatement().execute(
                "DROP TABLE users"
        );

        assertThrows(
                RepositoryException.class,
                () -> userRepository.findById(1)
        );
    }

    @Test
    void findAll_throwsRepositoryExceptionWhenUsersTableDoesNotExist()
            throws Exception {

        database.connection().createStatement().execute(
                "DROP TABLE users"
        );

        assertThrows(
                RepositoryException.class,
                () -> userRepository.findAll()
        );
    }

    @Test
    void save_throwsRepositoryExceptionWhenUsersTableDoesNotExist()
            throws Exception {

        database.connection().createStatement().execute(
                "DROP TABLE users"
        );

        assertThrows(
                RepositoryException.class,
                () -> userRepository.save(
                        new User(
                                0,
                                "caleb",
                                "password123",
                                null
                        )
                )
        );
    }

    @Test
    void findByUsername_throwsRepositoryExceptionWhenUsersTableDoesNotExist()
            throws Exception {

        database.connection().createStatement().execute(
                "DROP TABLE users"
        );

        assertThrows(
                RepositoryException.class,
                () -> userRepository.findByUsername("caleb")
        );
    }

    @Test
    void repository_throwsRepositoryExceptionWhenDatabaseIsClosed() {

        database.close();

        assertThrows(
                RepositoryException.class,
                () -> userRepository.findAll()
        );
    }
}