package com.rentaltracker.transport;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ListMenuTest {

    private final Function<String, String> row = s -> "row " + s;

    private ListMenu menuWithInput(String input) {
        Prompter prompter = new Prompter(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        return new ListMenu(prompter);
    }

    private List<String> items(int n) {
        return java.util.stream.IntStream.rangeClosed(1, n)
                .mapToObj(i -> "item" + i)
                .toList();
    }

    @Test
    void emptyList_returnsEmpty() {
        Optional<String> chosen = menuWithInput("").choose("T", List.of(), row);
        assertTrue(chosen.isEmpty());
    }

    @Test
    void picksFirstItemByNumber() {
        Optional<String> chosen = menuWithInput("1\n").choose("T", items(3), row);
        assertEquals(Optional.of("item1"), chosen);
    }

    @Test
    void picksLastItemOnFirstPage() {
        Optional<String> chosen = menuWithInput("5\n").choose("T", items(7), row);
        assertEquals(Optional.of("item5"), chosen);
    }

    @Test
    void backReturnsEmpty() {
        Optional<String> chosen = menuWithInput("b\n").choose("T", items(3), row);
        assertTrue(chosen.isEmpty());
    }

    @Test
    void previousPageThenPick() {
        // page1 -> n -> p -> pick 2 (item2)
        Optional<String> chosen = menuWithInput("n\np\n2\n").choose("T", items(7), row);
        assertEquals(Optional.of("item2"), chosen);
    }

    @Test
    void invalidNumber_reprompts() {
        // out-of-range -> then valid
        Optional<String> chosen = menuWithInput("99\n2\n").choose("T", items(3), row);
        assertEquals(Optional.of("item2"), chosen);
    }

    @Test
    void nonNumber_reprompts() {
        Optional<String> chosen = menuWithInput("x\n3\n").choose("T", items(3), row);
        assertEquals(Optional.of("item3"), chosen);
    }

    @Test
    void rendersRowText() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Prompter prompter = new Prompter(
                new ByteArrayInputStream("b\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8));
        new ListMenu(prompter).choose("MyTitle", List.of("a"), row);

        String printed = out.toString(StandardCharsets.UTF_8);
        assertTrue(printed.contains("=== MyTitle ==="));
        assertTrue(printed.contains("1) row a"));
        assertTrue(printed.contains("b) Back to menu"));
    }

    @Test
    void showsPageLabelWhenMultiplePages() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Prompter prompter = new Prompter(
                new ByteArrayInputStream("b\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8));
        new ListMenu(prompter).choose("T", items(6), row);

        assertTrue(out.toString(StandardCharsets.UTF_8).contains("(page 1 of 2)"));
    }
}