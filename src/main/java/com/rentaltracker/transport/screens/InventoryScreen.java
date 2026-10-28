package com.rentaltracker.transport.screens;

import java.util.List;
import java.util.Optional;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.service.ItemService;
import com.rentaltracker.transport.Display;
import com.rentaltracker.transport.ListMenu;
import com.rentaltracker.transport.Prompter;

public class InventoryScreen {
    private final Prompter prompter;
    private final ListMenu listMenu;
    private final ItemService items;

    public InventoryScreen(Prompter prompter, ItemService items) {
        this.prompter = prompter;
        this.listMenu = new ListMenu(prompter);
        this.items = items;
    }

    /** Every item the owner has, in any status. Reloaded each time so a status change shows up. */
    public void run(int ownerId) {
        while (true) {
            List<ItemDomain> owned = items.getItemsByOwner(ownerId);
            Optional<ItemDomain> picked = listMenu.choose("My inventory", owned,
                    item -> Display.row(item.getItemName(), item.getStatus()));
            if (picked.isEmpty()) {
                return;
            }
            // TODO: open the detail view
        }
    }
}
