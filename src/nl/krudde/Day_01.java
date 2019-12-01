package nl.krudde;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class Day_01 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        List<String> input = readFile(args);

        // part 1
        long totalFuel = input.stream()
                .map(Long::valueOf)
                .mapToLong(mass -> calculateFuel(mass))
                .sum();
        System.out.println("\npart 1: totalFuel = " + totalFuel);

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();

        long totalFuelForMassAndFuel = input.stream()
                .map(Long::valueOf)
                .mapToLong(mass -> calculateFuelForMassAndFuel(mass))
                .sum();
        System.out.println("\npart 2: totalFuelForMassAndFuel = " + totalFuelForMassAndFuel);

        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static long calculateFuelForMassAndFuel(long mass) {
        long totalFuel = 0;
        long fuel = mass;

        while (fuel > 0) {
            fuel = calculateFuel(fuel);
            totalFuel += fuel;
        }

        return totalFuel;
    }

    private static long calculateFuel(long mass) {
        long fuel = mass / 3 - 2;
        if (fuel < 0) {
            fuel = 0;
        }
        return fuel;
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
