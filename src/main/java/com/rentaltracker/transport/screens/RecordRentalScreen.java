package com.rentaltracker.transport.screens;

import java.util.List;
import java.util.Optional;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.RentalDomain;
import com.rentaltracker.service.RentalService;
import com.rentaltracker.service.exception.BusinessRuleException;
import com.rentaltracker.transport.Display;
import com.rentaltracker.transport.ListMenu;
import com.rentaltracker.transport.Prompter;

public class RecordRentalScreen {

    private final Prompter prompter;
    private final ListMenu listMenu;
    private final RentalService rentals;

    public RecordRentalScreen(Prompter prompter, RentalService rentals) {
        this.prompter = prompter;
        this.listMenu = new ListMenu(prompter);
        this.rentals = rentals;
    }

    public void run(int ownerId) {
        while (true) {
            List<ItemDomain> available = rentals.getRentableItems(ownerId);
            Optional<ItemDomain> picked = listMenu.choose("Record a rental", available,
                    item -> Display.row(item.getItemName(), item.getCostPerDay() + " per day"));
            if (picked.isEmpty()) {
                return;
            }
            rent(picked.get());
        }
    }

    private void rent(ItemDomain item) {
        prompter.println();
        prompter.println("=== " + item.getItemName() + " ===");
        prompter.println(Display.row("description", item.getDescription()));
        prompter.println(Display.row("cost per day", item.getCostPerDay()));
        prompter.println("1) Rent this item");
        prompter.println("2) Back to list");

        if (prompter.askChoice("> ", 1, 2) == 2) {
            return;
        }

        String renter = prompter.askNonBlank("Renter name: ");
        int days = prompter.askInt("Days: ");

        try {
            RentalDomain rental = rentals.recordRental(item.getItemId(), renter, days);
            prompter.println("Rented " + item.getItemName() + " to " + renter
                    + " until " + Display.time(rental.endTime()) + ".");
        } catch (IllegalArgumentException | BusinessRuleException e) {
            prompter.println(e.getMessage());
        }
    }
}
