package com.rentaltracker;


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Optional;

import com.rentaltracker.domain.User;
import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.*;
import com.rentaltracker.repository.exception.RepositoryException;
import com.rentaltracker.service.CurrentUserService;
import com.rentaltracker.service.RentalService;
import com.rentaltracker.service.UserService;
import com.rentaltracker.transport.MainMenu;
import com.rentaltracker.transport.Prompter;
import com.rentaltracker.transport.screens.RecordRentalScreen;
import com.rentaltracker.transport.screens.StartupScreen;

public class Main {
    public static void main(String[] args) {
        String path = args.length > 0 ? args[0] : "data/local.db";
        run(path, System.in, System.out, Clock.systemDefaultZone());
    }

    public static void run(String path, InputStream in, PrintStream out, Clock clock) {
        try {
            createParentDirectory(path);
            try (Database db = new Database(path)) {
                Prompter prompter = new Prompter(in, out);

                // 1. Identify the user (prompts only if app_state is empty)
                User me = new StartupScreen(db, prompter).show();

                // 2. Build and run the menu
                buildMenu(db, prompter, clock, me).run();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException | RepositoryException e) {
            out.println("Cannot run the app: " + e.getMessage());
        }
    }

    private static MainMenu buildMenu(Database db, Prompter prompter, Clock clock, User me) {
        UserRepository userRepository = new UserRepository(db);
        RentalRepository rentalRepository = new RentalRepository(db);
        ItemRepository itemRepository = new ItemRepository(db.connection());
        Transactor tx = new SqliteTransactor(db);

        UserService users = new UserService(userRepository);
        RentalService rentals = new RentalService(itemRepository, rentalRepository, users, tx, clock);

        return new MainMenu(prompter,
                new RecordRentalScreen(prompter, rentals));   // pass `me` if the screen needs it
    }


    private static void createParentDirectory(String path) throws IOException {
        Path parent = Path.of(path).toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
