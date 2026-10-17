package com.rentaltracker.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import com.rentaltracker.transport.exception.InputClosedException;

/** The only class that touches the console. Tests pass their own streams. */
public class Prompter {

    private final BufferedReader in;
    private final PrintStream out;

    public Prompter(InputStream in, PrintStream out) {
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
    }

    public void println(String text) { out.println(text); }

    public void println() { out.println(); }

    /** Prints the prompt and returns the next line, trimmed. */
    public String ask(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            String line = in.readLine();
            if (line == null) {
                throw new InputClosedException();
            }
            return line.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Asks until the answer is not blank. */
    public String askNonBlank(String prompt) {
        while (true) {
            String answer = ask(prompt);
            if (!answer.isEmpty()) {
                return answer;
            }
            println("Please enter a value.");
        }
    }

    /** Asks until the answer is a whole number. Range rules belong in the service. */
    public int askInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(ask(prompt));
            } catch (NumberFormatException e) {
                println("Please enter a whole number.");
            }
        }
    }

    /** Asks until the answer is a whole number from min to max. Used for menu choices. */
    public int askChoice(String prompt, int min, int max) {
        while (true) {
            try {
                int choice = Integer.parseInt(ask(prompt));
                if (choice >= min && choice <= max) {
                    return choice;
                }
            } catch (NumberFormatException e) {
                // falls through to the message below
            }
            println("Please enter a number from " + min + " to " + max + ".");
        }
    }
}
