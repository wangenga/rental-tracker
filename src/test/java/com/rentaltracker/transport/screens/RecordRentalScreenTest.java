package com.rentaltracker.transport.screens;

import com.rentaltracker.domain.ItemDomain;
import com.rentaltracker.domain.RentalDomain;
import com.rentaltracker.service.RentalService;
import com.rentaltracker.service.exception.BusinessRuleException;
import com.rentaltracker.transport.Prompter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecordRentalScreenTest {

    private Prompter prompter(String input, ByteArrayOutputStream out) {
        return new Prompter(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    private ItemDomain item() {
        // adapt to your ItemDomain shape
        return mock(ItemDomain.class,
                withSettings().stubOnly());
    }

    @Test
    void emptyAvailableList_doesNotPrompt() throws Exception {
        RentalService rentals = mock(RentalService.class);
        when(rentals.getRentableItems(1)).thenReturn(List.of());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        new RecordRentalScreen(prompter("", out), rentals).run(1);

        verify(rentals, never()).recordRental(anyInt(), anyString(), anyInt());
    }

    @Test
    void backFromList_exits() throws Exception {
        RentalService rentals = mock(RentalService.class);
        when(rentals.getRentableItems(1)).thenReturn(List.of(item()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        new RecordRentalScreen(prompter("b\n", out), rentals).run(1);

        verify(rentals, never()).recordRental(anyInt(), anyString(), anyInt());
    }

    @Test
    void backFromDetail_doesNotRecord() throws Exception {
        RentalService rentals = mock(RentalService.class);
        when(rentals.getRentableItems(1)).thenReturn(List.of(item()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // pick item 1, then "2) Back to list", then "b"
        new RecordRentalScreen(prompter("1\n2\nb\n", out), rentals).run(1);

        verify(rentals, never()).recordRental(anyInt(), anyString(), anyInt());
    }

    @Test
    void happyPath_recordsAndPrintsEndTime() throws Exception {
        RentalService rentals = mock(RentalService.class);
        ItemDomain it = item();
        when(it.getItemId()).thenReturn(7);
        when(it.getItemName()).thenReturn("Drill");
        when(it.getDescription()).thenReturn("18V cordless");
        when(it.getCostPerDay()).thenReturn((int) 5.0);
        when(rentals.getRentableItems(1)).thenReturn(List.of(it));

        RentalDomain rented = mock(RentalDomain.class);
        LocalDateTime end = LocalDateTime.of(2025, 1, 5, 12, 0);
        when(rented.endTime()).thenReturn(end);
        when(rentals.recordRental(7, "alice", 3)).thenReturn(rented);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // pick item -> rent -> renter -> days -> back
        new RecordRentalScreen(prompter("1\n1\nalice\n3\nb\n", out), rentals).run(1);

        verify(rentals).recordRental(7, "alice", 3);
        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("Rented Drill to alice until 2025-01-05 12:00."));
    }

    @Test
    void detailView_showsItemFields() throws Exception {
        RentalService rentals = mock(RentalService.class);
        ItemDomain it = item();
        when(it.getItemId()).thenReturn(7);
        when(it.getItemName()).thenReturn("Drill");
        when(it.getDescription()).thenReturn("18V cordless");
        when(it.getCostPerDay()).thenReturn((int) 5.0);
        when(rentals.getRentableItems(1)).thenReturn(List.of(it));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new RecordRentalScreen(prompter("1\n2\nb\n", out), rentals).run(1);

        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("=== Drill ==="));
        assertTrue(printed.contains("18V cordless"));
        assertTrue(printed.contains("1) Rent this item"));
        assertTrue(printed.contains("2) Back to list"));
    }

    @Test
    void businessRuleException_isPrintedNotThrown() throws Exception {
        RentalService rentals = mock(RentalService.class);
        ItemDomain it = item();
        when(it.getItemId()).thenReturn(7);
        when(it.getItemName()).thenReturn("Drill");
        when(it.getDescription()).thenReturn("d");
        when(it.getCostPerDay()).thenReturn((int) 5.0);
        when(rentals.getRentableItems(1)).thenReturn(List.of(it));
        when(rentals.recordRental(anyInt(), anyString(), anyInt()))
                .thenThrow(new BusinessRuleException("item not available"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new RecordRentalScreen(prompter("1\n1\nalice\n3\nb\n", out), rentals).run(1);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("item not available"));
    }

    @Test
    void illegalArgument_isPrintedNotThrown() throws Exception {
        RentalService rentals = mock(RentalService.class);
        ItemDomain it = item();
        when(it.getItemId()).thenReturn(7);
        when(it.getItemName()).thenReturn("Drill");
        when(it.getDescription()).thenReturn("d");
        when(it.getCostPerDay()).thenReturn((int) 5.0);
        when(rentals.getRentableItems(1)).thenReturn(List.of(it));
        when(rentals.recordRental(anyInt(), anyString(), anyInt()))
                .thenThrow(new IllegalArgumentException("days must be positive"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new RecordRentalScreen(prompter("1\n1\nalice\n0\nb\n", out), rentals).run(1);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("days must be positive"));
    }
}