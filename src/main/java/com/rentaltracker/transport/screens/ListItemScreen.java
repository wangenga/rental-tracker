package com.rentaltracker.transport.screens;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.service.ItemService;
import com.rentaltracker.transport.Prompter;

/**
 * Three prompts, then the item is listed as available
 * Validation errors go up to MainMenu
 */

public class ListItemScreen {

    private final Prompter prompter;
    private final ItemService items;

    public ListItemScreen(Prompter prompter, ItemService items) {
        this.prompter = prompter;
        this.items = items;
    }

    public void run(int ownerId) {
        prompter.print.ln();
        prompter.println("=== List an item ===");
        String name = prompter.askNonBlank("Name: ");
        String description = prompter.ask("Description: (optional): ");
        int costPerDay = prompter.askInt("Cost per day: ");

        ItemDomain item = items.createItem(ownerId, name, description, costPerDay);
        prompter.println("Listed " + item.getItemName() + " at " + item.getCostPerDay() + " per day.");
    }
}
