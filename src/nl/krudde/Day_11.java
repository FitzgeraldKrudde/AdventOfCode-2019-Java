package nl.krudde;

import lombok.Builder;
import lombok.Data;
import nl.krudde.Day_03.Point;
import nl.krudde.Day_11.Mode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

@Data
@Builder
class IntcodeV5 {
    private double[] program;
    private int position;
    private int relativeBase = 0;

    @Builder.Default
    private boolean halted = false;
    @Builder.Default
    private Queue<Integer> input = new LinkedList<>();
    @Builder.Default
    private Queue<Integer> output = new LinkedList<>();
    @Builder.Default
    private boolean initialised = false;

    public void addInput(Integer input) {
        this.input.add(input);
    }

    public int getOutput() {
        return output.remove();
    }

    public void run() {
        int opcode;
        double firstParameter, secondParameter;
        boolean waitingForInput = false;

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

public class Day_11 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        String input = readFile(args).get(0);

        // part 1
        System.out.println("\npart 1: ");
        double[] intCodeProgram = Arrays.stream(input.split(","))
                .mapToDouble(Double::valueOf)
                .toArray();

        IntcodeV5 intcode = new IntcodeV5.IntcodeV5Builder()
                .program(intCodeProgram)
                .build();

        final Map<Point, Color> map = new HashMap<>();
        Point currentLocation = new Point(0, 0);
        Day_03.Direction currentDirection = Day_03.Direction.U;

        map.put(currentLocation, Color.BLACK);
        intcode.addInput(0);

        while (!intcode.isHalted()) {
            intcode.run();
            Color color = Color.of(intcode.getOutput());
            map.put(currentLocation, color);
            Direction direction = Direction.of(intcode.getOutput());
            currentDirection = currentDirection.nextDirection(direction);
            currentLocation = currentLocation.nextPoint(currentDirection, 1);

            if (!map.containsKey(currentLocation)) {
                map.put(currentLocation, Color.BLACK);
            }
            intcode.addInput(map.get(currentLocation).ordinal());
        }

        int numberOfPanelsPainted = map.size();
        System.out.println("numberOfPanelsPainted = " + numberOfPanelsPainted);

        System.out.println();

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        intcode = new IntcodeV5.IntcodeV5Builder()
                .program(intCodeProgram)
                .build();

        map.clear();
        currentLocation = new Point(0, 0);
        currentDirection = Day_03.Direction.U;

        map.put(currentLocation, Color.BLACK);
        intcode.addInput(1);

        while (!intcode.isHalted()) {
            intcode.run();
            Color color = Color.of(intcode.getOutput());
            map.put(currentLocation, color);
            Direction direction = Direction.of(intcode.getOutput());
            currentDirection = currentDirection.nextDirection(direction);
            currentLocation = currentLocation.nextPoint(currentDirection, 1);

            if (!map.containsKey(currentLocation)) {
                map.put(currentLocation, Color.BLACK);
            }
            intcode.addInput(map.get(currentLocation).ordinal());
        }

        int maxX = map.keySet().stream()
                .mapToInt(Point::getX)
                .max()
                .orElseThrow(() -> new IllegalStateException("no max"));
        System.out.println("maxX = " + maxX);
        int maxY = map.keySet().stream()
                .mapToInt(Point::getY)
                .max()
                .orElseThrow(() -> new IllegalStateException("no max"));
        System.out.println("maxY = " + maxY);
        System.out.println();

        IntStream.rangeClosed(0, maxY)
                .forEach(y -> {
                    IntStream.rangeClosed(0, maxX)
                            .forEach(x -> {
                                if (x % (maxX / 8) == 0) {
                                    System.out.print(' ');
                                }
                                if (map.containsKey(new Point(x, y))) {
                                    switch (map.get(new Point(x, y))) {
                                        case BLACK -> System.out.print(' ');
                                        case WHITE -> System.out.print('X');
                                    }
                                } else {
                                    System.out.print(' ');
                                }
                            });
                    System.out.println();
                });

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

    enum Color {
        BLACK,
        WHITE;

        static Color of(int i) {
            if (i == 0) return BLACK;
            if (i == 1) return WHITE;
            throw new IllegalArgumentException("unknown color: " + i);
        }
    }

    enum Direction {
        LEFT90,
        RIGHT90;

        static Direction of(int i) {
            if (i == 0) return LEFT90;
            if (i == 1) return RIGHT90;
            throw new IllegalArgumentException("unknown direction: " + i);
        }
    }

    enum Mode {
        MODE_POSITION,
        MODE_IMMEDIATE,
        MODE_RELATIVE;

        static Mode of(int i) {
            if (i == 0) return MODE_POSITION;
            if (i == 1) return MODE_IMMEDIATE;
            if (i == 2) return MODE_RELATIVE;
            throw new IllegalArgumentException("unknown mode: " + i);
        }
    }
}
