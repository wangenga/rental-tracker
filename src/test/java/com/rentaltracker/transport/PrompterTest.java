package com.rentaltracker.transport;

import com.rentaltracker.transport.exception.InputClosedException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PrompterTest {

    private Prompter prompter(String input, ByteArrayOutputStream out) {
        return new Prompter(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    @Test
    void ask_returnsTrimmedLine() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals("hello", prompter("  hello  \n", out).ask("> "));
    }

    @Test
    void ask_printsPrompt() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        prompter("\n", out).ask("Name: ");
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Name: "));
    }

    @Test
    void ask_throwsWhenInputClosed() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(InputClosedException.class, () -> prompter("", out).ask("> "));
    }

    @Test
    void askNonBlank_skipsBlankLines() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String answer = prompter("\n   \nreal\n", out).askNonBlank("> ");
        assertEquals("real", answer);
    }

    @Test
    void askNonBlank_printsMessageOnBlank() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        prompter("\nok\n", out).askNonBlank("> ");
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Please enter a value."));
    }

    @Test
    void askInt_returnsWholeNumber() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(42, prompter("42\n", out).askInt("> "));
    }

    @Test
    void askInt_repromptsOnNonNumber() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int value = prompter("abc\n7\n", out).askInt("> ");
        assertEquals(7, value);
        assertTrue(out.toString(StandardCharsets.UTF_8)
                .contains("Please enter a whole number."));
    }

    @Test
    void askChoice_acceptsValueInRange() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(2, prompter("2\n", out).askChoice("> ", 1, 3));
    }

    @Test
    void askChoice_rejectsOutOfRange_thenAccepts() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int value = prompter("9\n2\n", out).askChoice("> ", 1, 3);
        assertEquals(2, value);
        assertTrue(out.toString(StandardCharsets.UTF_8)
                .contains("Please enter a number from 1 to 3."));
    }

    @Test
    void askChoice_rejectsNonNumber_thenAccepts() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(1, prompter("x\n1\n", out).askChoice("> ", 1, 3));
    }
}