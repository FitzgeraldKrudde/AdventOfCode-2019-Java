package nl.krudde;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class Day_22 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        List<String> input = readFile(args);

        // part 1
        LocalTime start = LocalTime.now();
        System.out.println("\npart 1: ");

        int deckSize = 10;
        deckSize = 10007;
        Deck deck = new Deck(deckSize);
        input.stream()
                .forEach(deck::handleLine);

        System.out.println("deck.getDeck().indexOf(2019) = " + deck.getDeck().indexOf(2019));

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");


        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static List<String> readFile(String[] args) throws IOException {
        String fileName;
        if (args.length == 0) {
            fileName = DEFAULT_FILENAME;
        } else {
            fileName = args[0];
        }

        System.out.println("reading file: " + fileName);
        // get the input lines
        List<String> input = Files.lines(Path.of(fileName)).collect(toList());
        System.out.println(String.format("read file: %s (#lines: %d)", fileName, input.size()));

        return input;
    }
}

@Data
class Deck {
    List<Integer> deck;

    Deck(int size) {
        deck = IntStream.range(0, size)
                .boxed()
                .collect(Collectors.toList());
    }

    public void dealIntoNewStack() {
        Collections.reverse(deck);
    }

    public void cut(int size) {
        if (size > 0) {
            List<Integer> cut = deck.subList(0, size);
            List<Integer> newDeck = new ArrayList<>(deck.subList(size, deck.size()));
            newDeck.addAll(cut);
            deck = newDeck;
        } else if (size < 0) {
            int s = Math.abs(size);
            List<Integer> newDeck = deck.subList(deck.size() - s, deck.size());
            List<Integer> remainder = new ArrayList<>(deck.subList(0, deck.size() - s));
            newDeck.addAll(remainder);
            deck = newDeck;
        }
    }

    public void dealWithIncrement(int incrementN) {
        List<Integer> newDeck = new ArrayList<>(deck);
        for (int i = 0; i < deck.size(); i++) {
            newDeck.set((i * incrementN) % deck.size(), deck.get(i));
        }
        deck = newDeck;
    }

    public void handleLine(String line) {
        if (line.startsWith("deal with increment")) {
            int increment = Integer.parseInt(line.split("\\s+")[3]);
            dealWithIncrement(increment);
        } else if (line.startsWith("cut")) {
            int cut = Integer.parseInt(line.split("\\s+")[1]);
            cut(cut);
        } else if (line.startsWith("deal into new stack")) {
            dealIntoNewStack();
        } else {
            throw new IllegalStateException("unknown line: " + line);
        }
    }
}
