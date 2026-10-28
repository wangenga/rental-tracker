package com.rentaltracker.transport.screens;

import com.rentaltracker.service.ItemService;
import com.rentaltracker.transport.Prompter;

public class InventoryScreen {
    private final Prompter prompter;
    private final ItemService items;

    public InventoryScreen(Prompter prompter, ItemService items) {
        this.prompter = prompter;
        this.items = items;
    }

    /** TODO(B): list the owner's items, open one for delist and relist. */
    public void run(int ownerId) {
        prompter.println("Inventory: not implemented yet.");
    }
}
