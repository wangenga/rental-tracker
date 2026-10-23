package com.rentaltracker.transport.screens;

import java.sql.SQLException;
import java.util.Optional;

import com.rentaltracker.domain.User;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.AppStateRepository;
import com.rentaltracker.repository.UserRepository;
import com.rentaltracker.service.CurrentUserService;
import com.rentaltracker.transport.Prompter;

/**
 * Checks whether an owner has been identified on this install.
 * On first launch, prompts for a username and claims the account.
 */
public class StartupScreen {

    private final CurrentUserService currentUser;
    private final Prompter prompter;

    public StartupScreen(Database db, Prompter prompter) {
        this.currentUser = new CurrentUserService(
                new AppStateRepository(db.connection()),   // ← not (Connection) db
                new UserRepository(db));
        this.prompter = prompter;
    }

    /** Returns the identified user, prompting on first launch. */
    public User show() throws SQLException {
        Optional<User> loaded = currentUser.loadCurrentUser();
        if (loaded.isPresent()) {
            prompter.println("Welcome back, " + loaded.get().getUsername() + "!");
            return loaded.get();
        }

        User me = currentUser.claim(prompter.askNonBlank("Username: "));
        prompter.println("Welcome, " + me.getUsername() + "!");
        return me;
    }
}