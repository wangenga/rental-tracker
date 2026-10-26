package com.rentaltracker.transport.screens;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.RentalDomain;
import com.rentaltracker.domain.User;
import com.rentaltracker.service.ItemService;
import com.rentaltracker.service.RentalService;
import com.rentaltracker.service.exception.BusinessRuleException;
import com.rentaltracker.transport.Display;
import com.rentaltracker.transport.ListMenu;
import com.rentaltracker.transport.Prompter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ConfirmReturnScreen {

    private final Prompter prompter;
    private final RentalService rental;
    private final ListMenu listMenu;

    public ConfirmReturnScreen (Prompter prompter, RentalService rental){
        this.prompter = prompter;
        this.rental = rental;
        this.listMenu = new ListMenu(prompter);
    }

    public void run (int ownerId){
        while (true) {
            List<RentalDomain> active = rental.getActiveRentals(ownerId);
            Optional<RentalDomain> returnRental = listMenu.choose("Confirm return", active,
                    rentals -> Display.row(String.valueOf(rentals.itemId()), rentals.renterId()));
            if (returnRental.isEmpty()){
                return;
            }
            confirmReturn(returnRental.get());
        }
    }
    public void confirmReturn (RentalDomain rentals){
        prompter.println();
        prompter.println("=== " + rentals.itemId() + " ===");
        prompter.println(Display.row("renter" , rentals.renterId()));
        prompter.println(Display.row("start", rentals.startTime()));
        prompter.println(Display.row("end", rentals.endTime()));
        prompter.println(Display.row("status", rentals.status()));
        prompter.println("1) Confirm return");
        prompter.println("2) Back to list");

        if (prompter.askChoice("> ", 1, 2) == 2) {
            return;
        }

        try{
            ItemDomain returnThisRental = rental.confirmReturn(rentals.id());
            prompter.println("Confirmed that "+ rentals.itemId() + " has been returned.");
        } catch (IllegalArgumentException | BusinessRuleException e) {
            prompter.println(e.getMessage());
        }
    }
}
