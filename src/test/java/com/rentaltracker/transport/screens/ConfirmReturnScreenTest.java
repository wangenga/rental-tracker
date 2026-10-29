package com.rentaltracker.transport.screens;

import com.rentaltracker.domain.enums.RentalStatus;
import com.rentaltracker.domain.view.ActiveRentalView;
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

class ConfirmReturnScreenTest {

    private Prompter prompter(String input, ByteArrayOutputStream out) {
        return new Prompter(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    private ActiveRentalView view() {
        return new ActiveRentalView(
                42,
                "Drill",
                "alice",
                LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2025, 1, 3, 10, 0),
                RentalStatus.ACTIVE);
    }


    @Test
    void choosingBackFromDetail_doesNotConfirm() throws Exception {
        RentalService rental = mock(RentalService.class);
        when(rental.getActiveRentalViews(1)).thenReturn(List.of(view()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // pick item 1, then "2) Back to list"; then "b" to exit list
        new ConfirmReturnScreen(prompter("1\n2\nb\n", out), rental).run(1);

        verify(rental, never()).confirmReturn(anyInt());
    }

    @Test
    void backFromList_exitsImmediately() throws Exception {
        RentalService rental = mock(RentalService.class);
        when(rental.getActiveRentalViews(1)).thenReturn(List.of(view()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        new ConfirmReturnScreen(prompter("b\n", out), rental).run(1);

        verify(rental, never()).confirmReturn(anyInt());
    }

    @Test
    void emptyList_doesNotPrompt() throws Exception {
        RentalService rental = mock(RentalService.class);
        when(rental.getActiveRentalViews(1)).thenReturn(List.of());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        new ConfirmReturnScreen(prompter("", out), rental).run(1);

        verify(rental, never()).confirmReturn(anyInt());
    }

    @Test
    void businessRuleException_isPrintedNotThrown() throws Exception {
        RentalService rental = mock(RentalService.class);
        when(rental.getActiveRentalViews(1)).thenReturn(List.of(view()));
        doThrow(new BusinessRuleException("already returned"))
                .when(rental).confirmReturn(42);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // pick item 1, confirm
        new ConfirmReturnScreen(prompter("1\n1\nb\n", out), rental).run(1);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("already returned"));
    }

    @Test
    void illegalArgument_isPrintedNotThrown() throws Exception {
        RentalService rental = mock(RentalService.class);
        when(rental.getActiveRentalViews(1)).thenReturn(List.of(view()));
        doThrow(new IllegalArgumentException("bad id")).when(rental).confirmReturn(42);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        new ConfirmReturnScreen(prompter("1\n1\nb\n", out), rental).run(1);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("bad id"));
    }

    @Test
    void detailView_showsFields() throws Exception {
        RentalService rental = mock(RentalService.class);
        when(rental.getActiveRentalViews(1)).thenReturn(List.of(view()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // view -> back -> back
        new ConfirmReturnScreen(prompter("1\n2\nb\n", out), rental).run(1);

        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("=== Drill ==="));
        assertTrue(printed.contains("alice"));
        assertTrue(printed.contains("1) Confirm return"));
        assertTrue(printed.contains("2) Back to list"));
    }
}