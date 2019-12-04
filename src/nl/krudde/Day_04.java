package nl.krudde;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class Day_04 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        List<String> input = readFile(args);

        // part 1
        System.out.println("\npart 1: ");
        int lowestNr = Integer.parseInt(input.get(0).split("-")[0]);
        int highestNr = Integer.parseInt(input.get(0).split("-")[1]);

        long count = IntStream.rangeClosed(lowestNr, highestNr)
                .filter(Day_04::twoAdjacentDigitsEqual)
                .filter(Day_04::digitsDoNotDecrease)
                .count();
        System.out.println("count = " + count);

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        count = IntStream.rangeClosed(lowestNr, highestNr)
                .filter(Day_04::strictlyTwoAdjacentDigitsEqual)
                .filter(Day_04::digitsDoNotDecrease)
                .count();
        System.out.println("count = " + count);
        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static boolean strictlyTwoAdjacentDigitsEqual(int number) {
        String s = String.valueOf(number);
        for (int i = 1; i < 6; i++) {
            if (s.charAt(i - 1) == s.charAt(i)) {
                // 2 adjacent digits equal, check if not part of greater group
                if (i > 1) {
                    if (s.charAt(i - 2) == s.charAt(i - 1)) {
                        continue;
                    }
                }
                if (i < 5) {
                    if (s.charAt(i + 1) == s.charAt(i)) {
                        continue;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static boolean digitsDoNotDecrease(int number) {
        String s = String.valueOf(number);
        for (int i = 1; i < 6; i++) {
            if (s.charAt(i) < s.charAt(i - 1)) {
                return false;
            }
        }
        return true;
    }

    private static boolean twoAdjacentDigitsEqual(int number) {
        String s = String.valueOf(number);
        for (int i = 1; i < 6; i++) {
            if (s.charAt(i - 1) == s.charAt(i)) {
                return true;
            }
        }
        return false;
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
