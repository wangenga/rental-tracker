package com.rentaltracker.transport;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** A numbered list, 5 per page, with next, previous and back. Returns the item the user picks. */
public class ListMenu {

    private final Prompter prompter;

    public ListMenu(Prompter prompter) {
        this.prompter = prompter;
    }

    /** @return the chosen item, or empty if the list is empty or the user went back */
    public <T> Optional<T> choose(String title, List<T> items, Function<T, String> row) {
        if (items.isEmpty()) {
            prompter.println("Nothing to show.");
            return Optional.empty();
        }

        PaginatedList<T> pages = new PaginatedList<>(items);
        while (true) {
            render(title, pages, row);
            String input = prompter.ask("> ").toLowerCase();

            if (input.equals("b")) {
                return Optional.empty();
            }
            if (input.equals("n") && pages.hasNext()) {
                pages.next();
                continue;
            }
            if (input.equals("p") && pages.hasPrevious()) {
                pages.previous();
                continue;
            }
            Optional<T> picked = pick(pages, input);
            if (picked.isPresent()) {
                return picked;
            }
            prompter.println("Invalid choice.");
        }
    }

    private <T> void render(String title, PaginatedList<T> pages, Function<T, String> row) {
        String pageLabel = pages.pageCount() > 1
                ? " (page " + pages.pageNumber() + " of " + pages.pageCount() + ")"
                : "";
        prompter.println();
        prompter.println("=== " + title + pageLabel + " ===");

        int number = pages.firstIndexOnPage();
        for (T item : pages.currentPage()) {
            number++;
            prompter.println(number + ") " + row.apply(item));
        }
        if (pages.hasNext()) prompter.println("n) Next page");
        if (pages.hasPrevious()) prompter.println("p) Previous page");
        prompter.println("b) Back to menu");
    }

    private <T> Optional<T> pick(PaginatedList<T> pages, String input) {
        try {
            int index = Integer.parseInt(input) - 1 - pages.firstIndexOnPage();
            List<T> current = pages.currentPage();
            return index >= 0 && index < current.size()
                    ? Optional.of(current.get(index))
                    : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}