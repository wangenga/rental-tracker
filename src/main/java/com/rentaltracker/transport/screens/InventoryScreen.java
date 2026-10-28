package com.rentaltracker.transport.screens;

import java.util.List;
import java.util.Optional;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.enums.ItemStatus;
import com.rentaltracker.service.ItemDetail;
import com.rentaltracker.service.ItemService;
import com.rentaltracker.service.exception.BusinessRuleException;
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
            showDetail(picked.get().getItemId());
        }
    }

    /** Shows the item's details and the owner's username, with an option to delist or relist it. */
    private void showDetail(int itemId) {
        ItemDetail detail = items.getItemDetail(itemId);
        ItemDomain item = detail.item();

        prompter.println();
        prompter.println("=== " + item.getItemName() + " ===");
        prompter.println(Display.row("description", item.getDescription()));
        prompter.println(Display.row("cost per day", item.getCostPerDay()));
        prompter.println(Display.row("status", item.getStatus()));
        prompter.println(Display.row("owner", detail.ownerUsername()));
        prompter.println(Display.row("listed", Display.time(item.getCreatedAt())));
        boolean unlisted = item.getStatus() == ItemStatus.unlisted;
        prompter.println(unlisted ? "1) Relist" : "1) Delist");
        prompter.println("2) Back to list");

        if (prompter.askChoice("> ", 1, 2) == 2) {
            return;
        }
        changeStatus(item, unlisted);
    }

    /** Relists an unlisted item, or delists any other. A refused change is shown and the user stays in the screen. */
    private void changeStatus(ItemDomain item, boolean relist) {
        try {
            ItemDomain updated = relist ? items.relist(item.getItemId()) : items.delist(item.getItemId());
            prompter.println(updated.getItemName() + " is now " + updated.getStatus() + ".");
        } catch (BusinessRuleException e) {
            prompter.println(e.getMessage());
        }
    }
}
