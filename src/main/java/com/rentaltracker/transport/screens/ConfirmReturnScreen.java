package com.rentaltracker.transport.screens;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.RentalDomain;
import com.rentaltracker.domain.User;
import com.rentaltracker.domain.view.ActiveRentalView;
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
    public void run(int ownerId) {
        while (true) {
            List<ActiveRentalView> active = rental.getActiveRentalViews(ownerId);
            Optional<ActiveRentalView> chosen = listMenu.choose("Confirm return", active,
                    v -> Display.row(v.itemName(), v.renterUsername()));
            if (chosen.isEmpty()) return;
            confirmReturn(chosen.get());
        }
    }

    public void confirmReturn(ActiveRentalView view) {
        prompter.println();
        prompter.println("=== " + view.itemName() + " ===");
        prompter.println(Display.row("renter", view.renterUsername()));
        prompter.println(Display.row("start",  view.startTime()));
        prompter.println(Display.row("end",    view.endTime()));
        prompter.println(Display.row("status", view.status()));
        prompter.println("1) Confirm return");
        prompter.println("2) Back to list");

        if (prompter.askChoice("> ", 1, 2) == 2) return;

        try {
            rental.confirmReturn(view.rentalId());
            prompter.println("Confirmed that " + view.itemName() + " has been returned.");
        } catch (IllegalArgumentException | BusinessRuleException e) {
            prompter.println(e.getMessage());
        }
    }
}
