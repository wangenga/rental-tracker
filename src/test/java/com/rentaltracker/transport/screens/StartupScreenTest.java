package com.rentaltracker.transport.screens;

import com.rentaltracker.domain.User;
import com.rentaltracker.service.UserService;
import com.rentaltracker.transport.Prompter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StartupScreenTest {

    /** Minimal User stub — replace with your real constructor if simpler. */
    private static User user(String name) {
        User u = mock(User.class);
        when(u.getUsername()).thenReturn(name);
        return u;
    }

    private Prompter prompterWithInput(String input) {
        return new Prompter(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    }

    @Test
    void existingOwner_welcomesAndReturnsIt() {
        UserService users = mock(UserService.class);
        User owner = user("alice");
        when(users.findOwner()).thenReturn(Optional.of(owner));

        User result = new StartupScreen(prompterWithInput(""), users).run();

        assertSame(owner, result);
        verify(users, never()).createOwner(anyString());
    }

    @Test
    void noOwner_createsOwnerFromUsername() {
        UserService users = mock(UserService.class);
        User created = user("bob");
        when(users.findOwner()).thenReturn(Optional.empty());
        when(users.createOwner("bob")).thenReturn(created);

        User result = new StartupScreen(prompterWithInput("bob\n"), users).run();

        assertSame(created, result);
        verify(users).createOwner("bob");
    }

    @Test
    void invalidName_repromptsUntilValid() {
        UserService users = mock(UserService.class);
        User created = user("carol");
        when(users.findOwner()).thenReturn(Optional.empty());
        when(users.createOwner("bad")).thenThrow(new IllegalArgumentException("taken"));
        when(users.createOwner("carol")).thenReturn(created);

        // first attempt "bad" fails, second "carol" succeeds
        User result = new StartupScreen(prompterWithInput("bad\ncarol\n"), users).run();

        assertSame(created, result);
        verify(users).createOwner("bad");
        verify(users).createOwner("carol");
    }

    @Test
    void blankUsername_isRejectedBeforeServiceIsCalled() {
        UserService users = mock(UserService.class);
        User created = user("dave");
        when(users.findOwner()).thenReturn(Optional.empty());
        when(users.createOwner("dave")).thenReturn(created);

        // blank line first — askNonBlank should skip it
        User result = new StartupScreen(prompterWithInput("\ndave\n"), users).run();

        assertSame(created, result);
        verify(users, never()).createOwner(eq(""));
    }
}