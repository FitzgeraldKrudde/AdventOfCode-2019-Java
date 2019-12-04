package nl.krudde;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Data
@AllArgsConstructor
class Intcode {
    private int[] program;
    private int noun;
    private int verb;

    public void run() {
        program[1] = noun;
        program[2] = verb;

        int position = 0;
        while (program[position] != 99) {
            program[program[position + 3]] =
                    switch (program[position]) {
                        case 1 -> program[program[position + 1]] + program[program[position + 2]];
                        case 2 -> program[program[position + 1]] * program[program[position + 2]];
                        default -> throw new IllegalStateException("unknown opcode: " + program[position]);
                    };
            position += 4;
        }
    }

    public int getPosition0() {
        return program[0];
    }
}

public class Day_02 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";


    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        String input = readFile(args).get(0);

        // part 1
        System.out.println("\npart 1: ");
        int[] intCodeProgram = Arrays.stream(input.split(","))
                .mapToInt(Integer::valueOf)
                .toArray();

        Intcode intcode = new Intcode(intCodeProgram.clone(), 12, 2);
        intcode.run();
        System.out.println("intCodeProgram[0] = " + intcode.getPosition0());

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();

        System.out.println("\npart 2: ");
        int result = 19690720;
        for (int noun = 0; noun <= 99; noun++) {
            for (int verb = 0; verb <= 99; verb++) {
                intcode = new Intcode(intCodeProgram.clone(), noun, verb);
                intcode.run();
                if (intcode.getPosition0() == result) {
                    System.out.println("intCodeProgram[0] = " + intcode.getPosition0());
                    System.out.println("noun = " + noun);
                    System.out.println("verb = " + verb);
                    System.out.println("100 * noun + verb = " + (100 * noun + verb));
                    finish = LocalTime.now();
                    System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
                    return;
                }
            }
        }

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
