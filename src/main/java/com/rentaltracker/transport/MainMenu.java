package com.rentaltracker.transport;

import com.rentaltracker.repository.exception.RepositoryException;
import com.rentaltracker.service.exception.BusinessRuleException;
import com.rentaltracker.transport.exception.InputClosedException;
import com.rentaltracker.transport.screens.ConfirmReturnScreen;
import com.rentaltracker.transport.screens.InventoryScreen;
import com.rentaltracker.transport.screens.ListItemScreen;
import com.rentaltracker.transport.screens.RecordRentalScreen;
import com.rentaltracker.transport.screens.StartupScreen;

public class MainMenu {

    private static final int EXIT = 5;

    private final Prompter prompter;
    private final RecordRentalScreen recordRental;
   

    public MainMenu(Prompter prompter, RecordRentalScreen recordRental) {
        this.prompter = prompter;
        this.recordRental = recordRental;
    }

    /** Runs until the user exits or the input ends. Never throws for expected failures. */
    public void run() {
        try {
            int ownerId = (int) startup.run().getId();
            while (true) {
                showMenu();
                int choice = prompter.askChoice("> ", 1, EXIT);
                if (choice == EXIT) {
                    prompter.println("Goodbye.");
                    return;
                }
                runScreen(choice, ownerId);
            }
        } catch (InputClosedException e) {
            prompter.println();
            prompter.println("Goodbye.");
        } catch (RepositoryException e) {
            prompter.println("Database problem: " + e.getMessage());
        }
    }

    private void showMenu() {
        prompter.println();
        prompter.println("=== Rental tracker ===");
        prompter.println("1) List an item");
        prompter.println("2) View my inventory");
        prompter.println("3) Record a rental");
        prompter.println("4) Confirm a return");
        prompter.println("5) Exit");
    }

    /** One failing screen reports the problem and returns to the menu. */
    private void runScreen(int choice, int ownerId) {
        try {
            switch (choice) {
                //case 1 -> 
                //case 2 -> 
                case 3 -> recordRental.run(ownerId);
                //default -> 
            }
        } catch (IllegalArgumentException | BusinessRuleException e) {
            prompter.println("Error: " + e.getMessage());
        } catch (RepositoryException e) {
            prompter.println("Database problem: " + e.getMessage());
        }
    }
}
