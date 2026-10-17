package com.rentaltracker.transport;

import java.util.List;

public class PaginatedList<T> {

    public static final int PAGE_SIZE = 5;

    private final List<T> items;
    private int page = 0;

    public PaginatedList(List<T> items) {
        this.items = List.copyOf(items);
    }

    public List<T> currentPage() {
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, items.size());
        return items.subList(from, to);
    }

    /** Zero-based index of the first item on this page. */
    public int firstIndexOnPage() { return page * PAGE_SIZE; }

    public boolean hasNext() { return (page + 1) * PAGE_SIZE < items.size(); }

    public boolean hasPrevious() { return page > 0; }

    public void next() {
        if (!hasNext()) throw new IllegalStateException("No next page");
        page++;
    }

    public void previous() {
        if (!hasPrevious()) throw new IllegalStateException("No previous page");
        page--;
    }

    public int pageNumber() { return page + 1; }

    public int pageCount() {
        return Math.max(1, (items.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }
}