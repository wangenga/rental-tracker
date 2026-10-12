package com.rentaltracker.service;

import com.rentaltracker.domain.User;
import com.rentaltracker.repository.UserRepository;
import com.rentaltracker.repository.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void createUser_shouldNormalizeUsernameBeforeSaving() {
        User savedUser = new User(
                1,
                "caleb",
                "password123",
                "2026-10-06T14:00:00"
        );

        when(userRepository.findByUsername("caleb"))
                .thenThrow(new NotFoundException("User not found"));

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        User result = userService.createUser(
                "  CALEB  ",
                "password123"
        );

        assertEquals("caleb", result.getUsername());

        verify(userRepository).findByUsername("caleb");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldRejectNullUsername() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(null, "password123")
        );

        assertEquals(
                "Username cannot be null",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void createUser_shouldRejectEmptyUsername() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("", "password123")
        );

        assertEquals(
                "Username cannot be empty",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void createUser_shouldRejectUsernameShorterThanThreeCharacters() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("ab", "password123")
        );

        assertEquals(
                "Username must be at least 3 characters",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void createUser_shouldRejectUsernameLongerThanFiftyCharacters() {
        String username = "a".repeat(51);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(username, "password123")
        );

        assertEquals(
                "Username must not exceed 50 characters",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void createUser_shouldAcceptUsernameWithLettersNumbersAndUnderscores() {
        User savedUser = new User(
                1,
                "caleb_123",
                "password123",
                "2026-10-06T14:00:00"
        );

        when(userRepository.findByUsername("caleb_123"))
                .thenThrow(new NotFoundException("User not found"));

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        User result = userService.createUser(
                "caleb_123",
                "password123"
        );

        assertEquals("caleb_123", result.getUsername());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldRejectUsernameContainingInvalidCharacters() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        "caleb-munyoki",
                        "password123"
                )
        );

        assertEquals(
                "Username may only contain lowercase letters, numbers, and underscores",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void createUser_shouldRejectDuplicateUsername() {
        User existingUser = new User(
                1,
                "caleb",
                "password123",
                "2026-10-06T14:00:00"
        );

        when(userRepository.findByUsername("caleb"))
                .thenReturn(existingUser);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        "caleb",
                        "newpassword"
                )
        );

        assertEquals(
                "Username already exists: caleb",
                exception.getMessage()
        );

        verify(userRepository).findByUsername("caleb");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_shouldRejectNullPassword() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        "caleb",
                        null
                )
        );

        assertEquals(
                "Password cannot be empty",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void createUser_shouldRejectBlankPassword() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(
                        "caleb",
                        "   "
                )
        );

        assertEquals(
                "Password cannot be empty",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void createUser_shouldSaveAndReturnUserWhenInputIsValid() {
        User savedUser = new User(
                5,
                "caleb",
                "password123",
                "2026-10-06T14:00:00"
        );

        when(userRepository.findByUsername("caleb"))
                .thenThrow(new NotFoundException("User not found"));

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        User result = userService.createUser(
                "caleb",
                "password123"
        );

        assertNotNull(result);
        assertEquals(5, result.getId());
        assertEquals("caleb", result.getUsername());
        assertEquals("password123", result.getPassword());

        verify(userRepository).findByUsername("caleb");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUser_shouldRejectZeroId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUser(0)
        );

        assertEquals(
                "User ID must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void getUser_shouldRejectNegativeId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUser(-1)
        );

        assertEquals(
                "User ID must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void getUser_shouldReturnUserFromRepository() {
        User user = new User(
                1,
                "caleb",
                "password123",
                "2026-10-06T14:00:00"
        );

        when(userRepository.findById(1))
                .thenReturn(user);

        User result = userService.getUser(1);

        assertSame(user, result);

        verify(userRepository).findById(1);
    }

    @Test
    void getUserByUsername_shouldNormalizeUsernameBeforeSearching() {
        User user = new User(
                1,
                "caleb",
                "password123",
                "2026-10-06T14:00:00"
        );

        when(userRepository.findByUsername("caleb"))
                .thenReturn(user);

        User result = userService.getUserByUsername(
                "  CALEB  "
        );

        assertSame(user, result);

        verify(userRepository).findByUsername("caleb");
    }

    @Test
    void getUserByUsername_shouldRejectInvalidUsername() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserByUsername("ab")
        );

        assertEquals(
                "Username must be at least 3 characters",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void getAllUsers_shouldReturnUsersFromRepository() {
        List<User> users = List.of(
                new User(1, "caleb", "password123", "2026-10-06T14:00:00"),
                new User(2, "john", "password456", "2026-10-06T14:01:00")
        );

        when(userRepository.findAll())
                .thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertEquals(users, result);

        verify(userRepository).findAll();
    }
}