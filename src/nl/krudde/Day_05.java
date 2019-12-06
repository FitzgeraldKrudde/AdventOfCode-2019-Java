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
import java.util.Scanner;

import static java.util.stream.Collectors.toList;

@Data
@AllArgsConstructor
class IntcodeV2 {
    final private int MODE_POSITION = 0;
    final private int MODE_IMMEDIATE = 1;
    private int[] program;

    public void run() {
        program = program.clone();

        int position = 0;
        int opcode;
        int firstParameter, secondParameter;
        long counter = 0;
        while (program[position] != 99) {
            opcode = getOpcode(position);
            switch (opcode) {
                case 1 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    program[program[position + 3]] = firstParameter + secondParameter;
                    position += 4;
                }
                case 2 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    program[program[position + 3]] = firstParameter * secondParameter;
                    position += 4;
                }
                case 3 -> {
                    Scanner scanner = new Scanner(System.in);
                    program[program[position + 1]] = scanner.nextInt();
                    scanner.close();
                    position += 2;
                }
                case 4 -> {
                    firstParameter = getParameter(position, 1);
                    System.out.println(firstParameter);
                    position += 2;
                }
                case 5 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    if (firstParameter != 0) {
                        position = secondParameter;
                    } else {
                        position += 3;
                    }
                }
                case 6 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    if (firstParameter == 0) {
                        position = secondParameter;
                    } else {
                        position += 3;
                    }
                }
                case 7 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    program[program[position + 3]] = firstParameter < secondParameter ? 1 : 0;
                    position += 4;
                }
                case 8 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    program[program[position + 3]] = firstParameter == secondParameter ? 1 : 0;
                    position += 4;
                }
                default -> throw new IllegalStateException("unknown opcode: " + opcode);
            }
            counter++;
        }
        System.out.println("counter = " + counter);
    }

    private int getOpcode(int position) {
        return program[position] % 100;
    }

    private int getParameter(int positionInstruction, int index) {
        int instruction = program[positionInstruction];
        int mode = (instruction / (10 * (int) Math.pow(10, index))) % 10;
        return mode == MODE_IMMEDIATE ? program[positionInstruction + index] : program[program[positionInstruction + index]];
    }
}

public class Day_05 {

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

        IntcodeV2 intcode = new IntcodeV2(intCodeProgram);
        intcode.run();
        System.out.println();

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
