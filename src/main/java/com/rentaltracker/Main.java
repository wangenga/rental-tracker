package com.rentaltracker;


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

import com.rentaltracker.infrastructure.Database;
import com.rentaltracker.repository.ItemRepository;
import com.rentaltracker.repository.RentalRepository;
import com.rentaltracker.repository.SqliteTransactor;
import com.rentaltracker.repository.Transactor;
import com.rentaltracker.repository.UserRepository;
import com.rentaltracker.repository.exception.RepositoryException;
import com.rentaltracker.service.RentalService;
import com.rentaltracker.service.UserService;
import com.rentaltracker.transport.MainMenu;
import com.rentaltracker.transport.Prompter;
import com.rentaltracker.transport.screens.RecordRentalScreen;

public class Main {
    public static void main(String[] args) {
        String path = args.length > 0 ? args[0] : "data/local.db";
        run(path, System.in, System.out, Clock.systemDefaultZone());
    }

    public static void run(String path, InputStream in, PrintStream out, Clock clock) {
        try {
            createParentDirectory(path);
            try (Database db = new Database(path)) {
                buildMenu(db, new Prompter(in, out), clock).run();
            }
        } catch (IOException | RepositoryException e) {
            out.println("Cannot run the app: " + e.getMessage());
        }
    }

    private static MainMenu buildMenu(Database db, Prompter prompter, Clock clock){
        UserRepository userRepository = new UserRepository(db);
        RentalRepository rentalRepository = new RentalRepository(db);
        ItemRepository itemRepository = new ItemRepository(db.connection());
        Transactor tx = new SqliteTransactor(db);

        UserService users = new UserService(userRepository);
        RentalService rentals = new RentalService(itemRepository, rentalRepository, users, tx, clock);
 
        return new MainMenu(prompter,
            new RecordRentalScreen(prompter, rentals)
        );
    }

    private static void createParentDirectory(String path) throws IOException {
        Path parent = Path.of(path).toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
