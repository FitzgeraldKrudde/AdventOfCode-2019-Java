package nl.krudde;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

@Data
@Builder
class IntcodeV3 {
    final private int MODE_POSITION = 0;
    final private int MODE_IMMEDIATE = 1;

    private int[] program;
    int position;

    private int amplifierPhase;
    @Setter
    private int input;
    @Getter
    private int amplifierOutputSignal;
    @Builder.Default
    private boolean firstInput = true;
    @Builder.Default
    private boolean halted = false;
    @Builder.Default
    private boolean initialised = false;

    public void run() {
        int opcode;
        int firstParameter, secondParameter;
        boolean waitingForInput = false;
        Optional<Integer> input = Optional.of(this.input);

        if (!initialised) {
            program = program.clone();
            initialised = true;
        }

        while (!halted && !waitingForInput) {
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
                    if (firstInput) {
                        program[program[position + 1]] = amplifierPhase;
                        firstInput = false;
                    } else if (input.isPresent()) {
                        program[program[position + 1]] = input.get();
                        input = Optional.empty();
                    } else {
                        waitingForInput = true;
                        break;
                    }
                    position += 2;
                }
                case 4 -> {
                    firstParameter = getParameter(position, 1);
                    amplifierOutputSignal = firstParameter;
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
            halted = program[position] == 99;
        }
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

public class Day_07 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    static int NR_AMPLIFIERS = 5;

    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        String input = readFile(args).get(0);

        // part 1
        System.out.println("\npart 1: ");
        int[] intCodeProgram = Arrays.stream(input.split(","))
                .mapToInt(Integer::valueOf)
                .toArray();

        long maxThrusterSignal = IntStream.rangeClosed(0, 44444)
                .parallel()
                .filter(Day_07::hasOnlyDistinctDigits)
                .filter(Day_07::containsOnlyDigitsLowerThan5)
                .mapToLong(i -> determineThrusterSignal(intCodeProgram, calculatePhaseAmplifiers(i)))
                .max()
                .orElseThrow(() -> new IllegalStateException("no value"));

        System.out.println("maxThrusterSignal = " + maxThrusterSignal);

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        maxThrusterSignal = IntStream.rangeClosed(55555, 99999)
                .parallel()
                .filter(Day_07::hasOnlyDistinctDigits)
                .filter(Day_07::containsOnlyDigitsGreaterOrEqualThan5)
                .mapToLong(i -> determineThrusterSignalWithFeedback(intCodeProgram, calculatePhaseAmplifiers(i)))
                .max()
                .orElseThrow(() -> new IllegalStateException("no value"));

        System.out.println("maxThrusterSignal = " + maxThrusterSignal);

        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static int[] calculatePhaseAmplifiers(int i) {
        int[] amplifierPhases = new int[NR_AMPLIFIERS];
        for (int j = 0; j < NR_AMPLIFIERS; j++) {
            amplifierPhases[4 - j] = (i / ((int) Math.pow(10, j))) % 10;
        }
        return amplifierPhases;
    }

    private static long determineThrusterSignal(int[] program, int[] amplifierPhases) {
        int amplifierOutputSignal = 0;
        for (int amplifier = 0; amplifier < NR_AMPLIFIERS; amplifier++) {
            IntcodeV3 intcode = new IntcodeV3.IntcodeV3Builder()
                    .program(program)
                    .amplifierPhase(amplifierPhases[amplifier])
                    .input(amplifierOutputSignal)
                    .build();
            intcode.run();
            amplifierOutputSignal = intcode.getAmplifierOutputSignal();
        }
        return amplifierOutputSignal;
    }

    private static long determineThrusterSignalWithFeedback(int[] program, int[] amplifierPhases) {
        int amplifierOutputSignal = 0;

        // create the amplifier programs
        IntcodeV3[] amplifierPrograms = new IntcodeV3[NR_AMPLIFIERS];
        for (int amplifier = 0; amplifier < NR_AMPLIFIERS; amplifier++) {
            amplifierPrograms[amplifier] = new IntcodeV3.IntcodeV3Builder()
                    .program(program)
                    .amplifierPhase(amplifierPhases[amplifier])
                    .build();
        }

        // run until the last program has halted
        while (!amplifierPrograms[NR_AMPLIFIERS - 1].isHalted()) {
            for (int j = 0; j < NR_AMPLIFIERS; j++) {
                amplifierPrograms[j].setInput(amplifierOutputSignal);
                amplifierPrograms[j].run();
                amplifierOutputSignal = amplifierPrograms[j].getAmplifierOutputSignal();
            }
        }
        return amplifierOutputSignal;
    }

    private static boolean containsOnlyDigitsGreaterOrEqualThan5(int i) {
        return String.valueOf(i).chars()
                .map(Character::getNumericValue)
                .filter(d -> d < 5)
                .count() == 0;
    }

    private static boolean containsOnlyDigitsLowerThan5(int i) {
        return String.valueOf(i).chars()
                .map(Character::getNumericValue)
                .filter(d -> d > 4)
                .count() == 0;
    }

    private static boolean hasOnlyDistinctDigits(int i) {
        String s = "00000" + i;
        s = s.substring(s.length() - 5);
        return s.chars()
                .map(Character::getNumericValue)
                .distinct()
                .count() == 5;
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
