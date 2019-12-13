package nl.krudde;

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
import static nl.krudde.JoystickMode.*;
import static nl.krudde.Tile.BLOCK;
import static nl.krudde.Tile.of;

@Data
@Builder
class IntcodeV6 {
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

public class Day_13 {

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

        IntcodeV6 intcode = new IntcodeV6.IntcodeV6Builder()
                .program(intCodeProgram)
                .build();
        intcode.run();

        int blocks = 0;
        while (intcode.hasOutput()) {
            int x = intcode.getOutput();
            int y = intcode.getOutput();
            Tile tile = of(intcode.getOutput());
            if (tile == BLOCK) {
                blocks++;
            }
        }
        System.out.println("blocks = " + blocks);
        System.out.println();

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        intcode = new IntcodeV6.IntcodeV6Builder()
                .program(intCodeProgram)
                .build();
        intcode.setMemoryZeroValue(2);
        intcode.addInput(0);

        int SIZE = 50;
        Tile[][] screen = new Tile[SIZE][SIZE];
        long score = 0;
        int nrJoystickInstructions = 0;
        Point ballLocation = null;
        Point previousBallLocation = null;
        Point paddleLocation = null;

        while (!intcode.isHalted()) {
            intcode.run();
            while (intcode.hasOutput()) {
                int x = intcode.getOutput();
                int y = intcode.getOutput();
                if (x == -1 && y == 0) {
                    score = intcode.getOutput();
                } else {
                    Tile tile = Tile.of(intcode.getOutput());
                    if (tile == Tile.BALL) {
                        ballLocation = new Point(x, y);
                    }
                    if (tile == Tile.PADDLE) {
                        paddleLocation = new Point(x, y);
                    }
                    screen[x][y] = tile;
                }
            }

            if (intcode.isWaitingForInput()) {
                JoystickMode joystickMode = JOYSTICK_NEUTRAL;
                // determine new input aka joystick movement instruction
                if (ballLocation != null && previousBallLocation == null) {
                    // first time ball on the screen
                    previousBallLocation = ballLocation;
                } else if (ballLocation != null && paddleLocation != null && previousBallLocation != null) {
                    if (paddleLocation.getX() > ballLocation.getX()) {
                        joystickMode = JOYSTICK_LEFT;
                    } else if (paddleLocation.getX() < ballLocation.getX()) {
                        joystickMode = JOYSTICK_RIGHT;
                    }
                    previousBallLocation = ballLocation;
                }
                intcode.addInput(joystickMode.getValue());
            }

            nrJoystickInstructions++;
        }

        System.out.println("\nscore = " + score);
        System.out.println("nrJoystickInstructions = " + nrJoystickInstructions);
        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static void printScreen(Tile[][] screen) {
        for (int y = 0; y < 22; y++) {
            for (int x = 0; x < screen[0].length; x++) {
                Tile tile = screen[x][y];
                if (tile != null) {
                    System.out.print(tile.getPrintCharacter());
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

enum Tile {
    EMPTY(' '),
    WALL('#'),
    BLOCK('.'),
    PADDLE('P'),
    BALL('B');

    @Getter
    private final char printCharacter;

    Tile(char printCharacter) {
        this.printCharacter = printCharacter;
    }

    static Tile of(int type) {
        return switch (type) {
            case 0 -> EMPTY;
            case 1 -> WALL;
            case 2 -> BLOCK;
            case 3 -> PADDLE;
            case 4 -> BALL;
            default -> throw new IllegalStateException("unknown tile type");
        };
    }
}

enum JoystickMode {
    JOYSTICK_NEUTRAL,
    JOYSTICK_LEFT,
    JOYSTICK_RIGHT;

    public int getValue() {
        return switch (this) {
            case JOYSTICK_NEUTRAL -> 0;
            case JOYSTICK_LEFT -> -1;
            case JOYSTICK_RIGHT -> 1;
        };
    }
}
