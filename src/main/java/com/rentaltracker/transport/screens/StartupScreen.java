package com.rentaltracker.transport.screens;

import java.util.Optional;

import com.rentaltracker.domain.User;
import com.rentaltracker.service.UserService;
import com.rentaltracker.transport.Prompter;

/**
 * The owner is the lowest-id user. On first launch there is no user yet,
 * so the screen asks for a username and creates the owner account.
 */
public class StartupScreen {

    private final Prompter prompter;
    private final UserService users;

    public StartupScreen(Prompter prompter, UserService users) {
        this.prompter = prompter;
        this.users = users;
    }

    public User run() {
        Optional<User> owner = users.findOwner();
        if (owner.isPresent()) {
            prompter.println("Welcome back, " + owner.get().getUsername() + ".");
            return owner.get();
        }

        prompter.println("Welcome! Let's set up your account.");
        while (true) {
            String name = prompter.askNonBlank("Username: ");
            try {
                User created = users.createOwner(name);
                prompter.println("Account created. Hello, " + created.getUsername() + ".");
                return created;
            } catch (IllegalArgumentException e) {
                prompter.println(e.getMessage());
            }
        }
    }
}