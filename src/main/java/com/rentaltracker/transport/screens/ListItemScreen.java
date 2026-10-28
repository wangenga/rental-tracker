package com.rentaltracker.transport.screens;

import com.rentaltracker.service.ItemService;
import com.rentaltracker.transport.Prompter;

public class ListItemScreen {
    private final Prompter prompter;
    private final ItemService items;

    public ListItemScreen(Prompter prompter, ItemService items) {
        this.prompter = prompter;
        this.items = items;
    }

    /** TODO(B): ask for name, description and cost per day, then call items.createItem. */
    public void run(int ownerId) {
        prompter.println("List an item: not implemented yet.");
    }
}
