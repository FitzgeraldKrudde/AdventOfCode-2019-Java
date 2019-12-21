package nl.krudde;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.util.stream.Collectors.toList;

@Data
@Builder
class IntcodeV10 {
    private double[] program;
    private int position;
    private int relativeBase;

    @Builder.Default
    private boolean halted = false;
    @Builder.Default
    boolean waitingForInput = false;
    @Builder.Default
    private Queue<Integer> input = new LinkedList<>();
    @Builder.Default
    private Queue<Integer> output = new LinkedList<>();
    @Builder.Default
    private boolean initialised = false;

    public void addInput(Integer input) {
        this.input.add(input);
        waitingForInput = false;
    }

    public boolean hasOutput() {
        return !output.isEmpty();
    }

    public int getOutput() {
        return output.remove();
    }

    public void setMemoryZeroValue(int value) {
        program[0] = value;
    }

    public void run() {
        int opcode;
        double firstParameter, secondParameter;

        if (!initialised) {
            double[] inputProgram = program;
            // make space for 256K memory above the program
            program = new double[program.length + 256 * 1024];
            // copy the program
            System.arraycopy(inputProgram, 0, program, 0, inputProgram.length);
            initialised = true;
        }

        while (!halted && !waitingForInput) {
            opcode = getOpcode(position);
            switch (opcode) {
                case 1 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    int writePosition = getPosition(position, 3);
                    program[writePosition] = firstParameter + secondParameter;
                    position += 4;
                }
                case 2 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    int writePosition = getPosition(position, 3);
                    program[writePosition] = firstParameter * secondParameter;
                    position += 4;
                }
                case 3 -> {
                    if (input.peek() == null) {
                        waitingForInput = true;
                    } else {
                        waitingForInput = false;

                        int writePosition = getPosition(position, 1);
                        program[writePosition] = input.remove();
                        position += 2;
                    }
                }
                case 4 -> {
                    firstParameter = getParameter(position, 1);
                    output.add((int) firstParameter);
                    position += 2;
                }
                case 5 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    if (firstParameter != 0) {
                        position = (int) secondParameter;
                    } else {
                        position += 3;
                    }
                }
                case 6 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    if (firstParameter == 0) {
                        position = (int) secondParameter;
                    } else {
                        position += 3;
                    }
                }
                case 7 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    int writePosition = getPosition(position, 3);
                    program[writePosition] = firstParameter < secondParameter ? 1 : 0;
                    position += 4;
                }
                case 8 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    int writePosition = getPosition(position, 3);
                    program[writePosition] = firstParameter == secondParameter ? 1 : 0;
                    position += 4;
                }
                case 9 -> {
                    firstParameter = getParameter(position, 1);
                    relativeBase += firstParameter;
                    position += 2;
                }
                default -> throw new IllegalStateException("unknown opcode: " + opcode);
            }
            halted = program[position] == 99;
        }
    }

    private int getOpcode(int position) {
        return (int) (program[position] % 100);
    }

    private double getParameter(int positionInstruction, int index) {
        return program[getPosition(positionInstruction, index)];
    }

    private int getPosition(int positionInstruction, int index) {
        int instruction = (int) program[positionInstruction];
        int mode = (int) ((instruction / (10 * (int) Math.pow(10, index))) % 10);
        return switch (Mode.of(mode)) {
            case MODE_IMMEDIATE -> positionInstruction + index;
            case MODE_POSITION -> (int) program[positionInstruction + index];
            case MODE_RELATIVE -> (int) (program[positionInstruction + index] + relativeBase);
            default -> throw new IllegalStateException("unknown mode: " + mode);
        };
    }
}

public class Day_21 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    int runs = 0;

    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        String input = readFile(args).get(0);

        // part 1
        System.out.println("\npart 1: ");
        double[] intCodeProgram = Arrays.stream(input.split(","))
                .mapToDouble(Double::valueOf)
                .toArray();
        IntcodeV10 intcode;
        intcode = new IntcodeV10.IntcodeV10Builder()
                .program(intCodeProgram)
                .build();

        // truth table:
        //      Jump
        //@####  F
        //@###.  F
        //@#.##  F
        //@##..  F
        //@#.#.  F
        //@#...  F
        //
        //@##.#  T
        //@.###  T
        //@#..#  T
        //@..##  T
        //@.#.#  T
        //@#..#  T
        //@...#  T
        //
        //do not jump if A true en (D is false or C is true)
        //OR D T
        //NOT T T
        //OR C T
        //        T-true wanneer (D false or C true)
        //AND A T
        //        T-true wanneer A true en (D is false or C true)
        //NOT T T
        //OR T J

        String springscriptProgram = """
                OR D T
                NOT T T
                OR C T
                AND A T
                NOT T T
                OR T J""";
        String springscriptCommandWalk = "WALK";
        sendSpringscriptCommand(intcode, springscriptProgram);
        sendSpringscriptCommand(intcode, springscriptCommandWalk);
        intcode.run();
        while (intcode.hasOutput()) {
            printOutput(intcode);
        }

        System.out.println();

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        intCodeProgram = Arrays.stream(input.split(","))
                .mapToDouble(Double::valueOf)
                .toArray();
        intcode = new IntcodeV10.IntcodeV10Builder()
                .program(intCodeProgram)
                .build();

        springscriptProgram = """
                NOT A T
                NOT T T
                AND B T
                AND C T
                NOT T J
                AND D J
                NOT E T
                NOT T T
                OR  H T
                AND T J""";
        String springscriptCommandRun = "RUN";
        sendSpringscriptCommand(intcode, springscriptProgram);
        sendSpringscriptCommand(intcode, springscriptCommandRun);
        intcode.run();
        while (intcode.hasOutput()) {
            printOutput(intcode);
        }
        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static void sendSpringscriptCommand(IntcodeV10 intcode, String springscriptProgram) {
        springscriptProgram.chars()
                .forEach(c -> intcode.addInput((int) c));
        intcode.addInput(10); // newline
    }

    private static void printOutput(IntcodeV10 intcode) {
        int output = intcode.getOutput();
        if (output < 128) {
            // ASCII
            System.out.print(Character.toString(output));
        } else System.out.println("output = " + output);
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

