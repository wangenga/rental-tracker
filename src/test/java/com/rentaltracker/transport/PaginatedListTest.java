package com.rentaltracker.transport;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PaginatedListTest {

    private List<Integer> items(int n) {
        return IntStream.rangeClosed(1, n).boxed().toList();
    }

    @Test
    void emptyList_hasOnePage_andNoItems() {
        PaginatedList<Integer> p = new PaginatedList<>(List.of());
        assertEquals(1, p.pageCount());
        assertTrue(p.currentPage().isEmpty());
        assertFalse(p.hasNext());
        assertFalse(p.hasPrevious());
    }

    @Test
    void firstPage_holdsUpToFiveItems() {
        PaginatedList<Integer> p = new PaginatedList<>(items(7));
        assertEquals(List.of(1, 2, 3, 4, 5), p.currentPage());
        assertEquals(0, p.firstIndexOnPage());
        assertEquals(2, p.pageCount());
        assertTrue(p.hasNext());
        assertFalse(p.hasPrevious());
    }

    @Test
    void next_movesToSecondPage() {
        PaginatedList<Integer> p = new PaginatedList<>(items(7));
        p.next();
        assertEquals(List.of(6, 7), p.currentPage());
        assertEquals(5, p.firstIndexOnPage());
        assertEquals(2, p.pageNumber());
        assertFalse(p.hasNext());
        assertTrue(p.hasPrevious());
    }

    @Test
    void previous_returnsToFirstPage() {
        PaginatedList<Integer> p = new PaginatedList<>(items(7));
        p.next();
        p.previous();
        assertEquals(List.of(1, 2, 3, 4, 5), p.currentPage());
        assertEquals(1, p.pageNumber());
    }

    @Test
    void next_whenNoNextPage_throws() {
        PaginatedList<Integer> p = new PaginatedList<>(items(3));
        assertThrows(IllegalStateException.class, p::next);
    }

    @Test
    void previous_whenNoPreviousPage_throws() {
        PaginatedList<Integer> p = new PaginatedList<>(items(3));
        assertThrows(IllegalStateException.class, p::previous);
    }

    @Test
    void constructor_copiesList() {
        java.util.ArrayList<Integer> source = new java.util.ArrayList<>(List.of(1, 2, 3));
        PaginatedList<Integer> p = new PaginatedList<>(source);
        source.clear();
        assertEquals(3, p.currentPage().size());
    }
}