package com.rentaltracker.transport.screens;

import com.rentaltracker.service.ItemService;
import com.rentaltracker.transport.Prompter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryScreenTest {

    @Test
    void run_printsNotImplementedMessage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Prompter prompter = new Prompter(
                new ByteArrayInputStream(new byte[0]),
                new PrintStream(out, true, StandardCharsets.UTF_8));

        new InventoryScreen(prompter, null).run(1);

        assertTrue(out.toString(StandardCharsets.UTF_8)
                .contains("Inventory: not implemented yet."));
    }
}