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
class IntcodeV9 {
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

public class Day_19 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    int runs=0;
    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        String input = readFile(args).get(0);

        // part 1
        System.out.println("\npart 1: ");
        double[] intCodeProgram = Arrays.stream(input.split(","))
                .mapToDouble(Double::valueOf)
                .toArray();

        IntcodeV9 intcode;
        int gridSize = 50;
        int[][] grid = new int[gridSize][gridSize];
        int nrPointsAffected = 0;
        for (int x = 0; x < gridSize; x++) {
            for (int y = 0; y < gridSize; y++) {
                intcode = new IntcodeV9.IntcodeV9Builder()
                        .program(intCodeProgram)
                        .build();
                intcode.addInput(x);
                intcode.addInput(y);
                intcode.run();
                int output = intcode.getOutput();
                grid[x][y] = output;
                if (output == 1) {
                    nrPointsAffected++;
                }
            }
        }
//        print(grid);
        System.out.println("nrPointsAffected = " + nrPointsAffected);

        System.out.println();

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        boolean found = false;
        int minX = 4;
        int maxX = 4;
        int x = minX;
        int y = 3;

        // search for horizontal length >= 100
        // by following the upper left and right edges
        // define rectangle, use upper left as base point
        // check for 100x100 by checking lower left/right
        while (!found) {
            // find horizontal start
            while (getOutput(intCodeProgram, x, y) == 0) {
                x++;
                minX++;
            }
            // find horizontal end
            x = maxX;
            while (getOutput(intCodeProgram, x, y) == 1) {
                x++;
                maxX++;
            }
            int length = maxX - minX;
            if (length >= 200) {
                // move right and check for rectangle
                for (int upperLeftx = minX; upperLeftx < maxX - 99; upperLeftx++) {
                    if (getOutput(intCodeProgram, upperLeftx, y + 99) == 1 && getOutput(intCodeProgram, upperLeftx + 99, y + 99) == 1) {
                        System.out.println("FOUND!! upperLeftx * 10000 + y = " + (upperLeftx * 10000 + y));
                        found = true;
                    }
                }
            }
            // next line
            y++;
            x = minX;
        }

        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static int getOutput(double[] intCodeProgram, int x, int y) {
        IntcodeV9 intcode;
        intcode = new IntcodeV9.IntcodeV9Builder()
                .program(intCodeProgram)
                .build();
        intcode.addInput(x);
        intcode.addInput(y);
        intcode.run();
        return intcode.getOutput();
    }

    private static int sumRow(int[][] grid, int y) {
        int sum = 0;
        for (int x = 0; x < grid[0].length; x++) {
            sum += grid[x][y];
        }
        return sum;
    }

    private static int rectangleSum(int[][] grid, int x, int y) {
        int sum = 0;
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                if (grid[x + i][y + j] == 1) {
                    sum++;
                } else {
                    return -1;
                }
            }
        }
        return sum;
    }

    static private void print(int[][] grid) {
        for (int y = 0; y < grid.length; y++) {
            System.out.print(String.format("%3d ", y));
            for (int x = 0; x < grid.length; x++) {
                if (grid[x][y] == 0) {
                    System.out.print('.');
                } else {
                    System.out.print(grid[x][y]);
                }
            }
            System.out.println();
        }
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

@AllArgsConstructor
enum DroneStatus {
    STATIONARY('.'),
    PULLED('#'),
    ;

    @Getter
    private final char printCharacter;

    static DroneStatus of(int i) {
        return switch (i) {
            case 0 -> STATIONARY;
            case 1 -> PULLED;
            default -> throw new IllegalStateException("unknown type " + i);
        };
    }
}
