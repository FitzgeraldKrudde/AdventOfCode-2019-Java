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
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;
import static nl.krudde.Direction.*;

@Data
@Builder
class IntcodeV8 {
    private long[] program;
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

    public long getOutput() {
        return output.remove();
    }

    public void setMemoryZeroValue(int value) {
        program[0] = value;
    }

    public void run() {
        int opcode;
        long firstParameter, secondParameter;

        if (!initialised) {
            long[] inputProgram = program;
            // make space for 256K memory above the program
            program = new long[program.length + 256 * 1024];
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

    private long getParameter(int positionInstruction, int index) {
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

public class Day_17 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        String input = readFile(args).get(0);

        // part 1
        System.out.println("\npart 1: ");
        long[] intCodeProgram = Arrays.stream(input.split(","))
                .mapToLong(Long::valueOf)
                .toArray();

        IntcodeV8 intcode = new IntcodeV8.IntcodeV8Builder()
                .program(intCodeProgram)
                .build();

        VacuumRobotComputer vacuumRobotComputer = new VacuumRobotComputer(intcode);
        vacuumRobotComputer.exploreArea();
        int sumAlignmentParameters = vacuumRobotComputer.sumAlignmentParameters();
        System.out.println("sumAlignmentParameters = " + sumAlignmentParameters);

        System.out.println();

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        List<String> segmentspath = vacuumRobotComputer.determineSegments();
        System.out.println("segmentspath = " + segmentspath.toString().replaceAll(", ", ""));
        // L12L12L6L6R8R4L12L12L12L6L6L12L6R12R8R8R4L12L12L12L6L6L12L6R12R8R8R4L12L12L12L6L6L12L6R12R8

        intcode = new IntcodeV8.IntcodeV8Builder()
                .program(intCodeProgram)
                .build();

        vacuumRobotComputer = new VacuumRobotComputer(intcode);
        int nrDust = vacuumRobotComputer.walkAllScaffoldsAndCollectDust();
        System.out.println("nrDust = " + nrDust);

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

@AllArgsConstructor
enum MapCoordinateType {
    SCAFFOLD('#'),
    OPEN('.'),
    ;

    @Getter
    private final char printCharacter;

    static MapCoordinateType of(int i) {
        return switch (i) {
            case 35 -> SCAFFOLD;
            case 46 -> OPEN;
            default -> throw new IllegalStateException("unknown type " + i);
        };
    }
}

@Data
class VacuumRobotComputer {
    private IntcodeV8 program;

    Map<Point, MapCoordinateType> area = new TreeMap<>();

    private Point vacuumRobotLocation;
    private Direction vacuumRobotDirection;

    VacuumRobotComputer(IntcodeV8 program) {
        this.program = program;
    }

    public void exploreArea() {
        int x = 0;
        int y = 0;

        program.run();
        while (program.hasOutput()) {
            long i = program.getOutput();
            switch ((int) i) {
                case 10 -> {
                    y++;
                    x = 0;
                }
                case 94, 118, 60, 62 -> {
                    vacuumRobotLocation = new Point(x, y);
                    area.put(new Point(x, y), MapCoordinateType.SCAFFOLD);
                    vacuumRobotDirection = switch ((int) i) {
                        case 94 -> U;
                        case 118 -> D;
                        case 60 -> L;
                        case 62 -> R;
                        default -> throw new IllegalStateException("unknown character: " + i);
                    };
                    x++;
                }
                default -> {
                    area.put(new Point(x, y), MapCoordinateType.of((int) i));
                    x++;
                }
            }
        }

//        print();
    }

    public List<Point> getIntersections() {
        return area.entrySet().stream()
                .filter(entry -> entry.getValue() == MapCoordinateType.SCAFFOLD)
                .filter(entry -> isIntersection(entry.getKey()))
                .map(Map.Entry::getKey)
                .collect(toList());
    }

    private void print() {
        System.out.println("\nx: " + minX() + "->" + maxX() + " y: " + minY() + "->" + maxY() + "\n");

        IntStream.rangeClosed(minY(), maxY())
                .forEach(y -> {
                    IntStream.rangeClosed(minX(), maxX())
                            .forEach(x -> {
                                Point p = new Point(x, y);
                                if (p.equals(vacuumRobotLocation)) {
                                    char c =
                                            switch (vacuumRobotDirection) {
                                                case L -> c = '<';
                                                case R -> c = '>';
                                                case U -> c = '^';
                                                case D -> c = 'v';
                                            };
                                    System.out.print(c);
                                } else if (area.containsKey(p)) {
                                    System.out.print(area.get(p).getPrintCharacter());
                                } else {
                                    System.out.print(' ');
                                }
                            });
                    System.out.println();
                });
    }

    private int minX() {
        return area.keySet().stream()
                .min(Comparator.comparingInt(Point::getX))
                .orElseThrow(() -> new IllegalStateException("no min x"))
                .getX();
    }

    private int maxX() {
        return area.keySet().stream()
                .max(Comparator.comparingInt(Point::getX))
                .orElseThrow(() -> new IllegalStateException("no max x"))
                .getX();
    }

    private int minY() {
        return area.keySet().stream()
                .min(Comparator.comparingInt(Point::getY))
                .orElseThrow(() -> new IllegalStateException("no min y"))
                .getY();
    }

    private int maxY() {
        return area.keySet().stream()
                .max(Comparator.comparingInt(Point::getY))
                .orElseThrow(() -> new IllegalStateException("no max y"))
                .getY();
    }

    private boolean isIntersection(Point point) {
        return Arrays.stream(values())
                .filter(direction -> isScaffold(point.nextPoint(direction)))
                .count() == 4;
    }

    private boolean isScaffold(Point point) {
        return area.containsKey(point) && area.get(point).equals(MapCoordinateType.SCAFFOLD);
    }

    public int sumAlignmentParameters() {
        return getIntersections().stream()
                .mapToInt(this::calculateAlignmentParameter)
                .sum();
    }

    private int calculateAlignmentParameter(Point point) {
        return point.getX() * point.getY();
    }

    private int nrScaffolds() {
        return (int) area.values().stream()
                .filter(type -> type == MapCoordinateType.SCAFFOLD)
                .count();
    }

    public List<String> determineSegments() {
        List<String> segmentList = new ArrayList<>();
        Point currentPoint = vacuumRobotLocation;
        Point startingPoint = currentPoint;
        Direction currentDirection = vacuumRobotDirection;
        Direction nextDirection = vacuumRobotDirection;
        String turn = null;

        while (nextDirection != null) {
            // walk as far as possible in current direction
            while (isScaffold(currentPoint.nextPoint(currentDirection))) {
                currentPoint = currentPoint.nextPoint(currentDirection);
            }
            int length = startingPoint.manhattanDistance(currentPoint);
            if (length > 0) {
                segmentList.add(turn + length);
                startingPoint = currentPoint;
            }
            // take a turn
            nextDirection = getNextDirectionForTurning(currentPoint, currentDirection);
            if (nextDirection != null) {
                turn = determineTurn(currentDirection, nextDirection);
                currentDirection = nextDirection;
            }
        }


        return segmentList;
    }

    private String determineTurn(Direction currentDirection, Direction nextDirection) {
        if (currentDirection.equals(U) && nextDirection.equals(L)) {
            return "L";
        }
        if (currentDirection.equals(U) && nextDirection.equals(R)) {
            return "R";
        }
        if (currentDirection.equals(D) && nextDirection.equals(L)) {
            return "R";
        }
        if (currentDirection.equals(D) && nextDirection.equals(R)) {
            return "L";
        }
        if (currentDirection.equals(L) && nextDirection.equals(U)) {
            return "R";
        }
        if (currentDirection.equals(L) && nextDirection.equals(D)) {
            return "L";
        }
        if (currentDirection.equals(R) && nextDirection.equals(U)) {
            return "L";
        }
        if (currentDirection.equals(R) && nextDirection.equals(D)) {
            return "R";
        }
        throw new IllegalStateException("illegal combination of directions: " + currentDirection + "-" + nextDirection);
    }

    private Direction getNextDirectionForTurning(Point point, Direction currentDirection) {
        return Arrays.stream(values())
                .filter(direction -> direction != currentDirection)
                .filter(direction -> direction != currentDirection.oppositeDirection())
                .filter(direction -> isScaffold(point.nextPoint(direction)))
                .findFirst()
                .orElse(null);
    }

    private String sendCommandAndReceiveOutput(String command) {
        sendCommand(command);
        return getOutput();
    }

    private void sendCommand(String command) {
        (command + "\n").chars().boxed()
                .forEach(i -> program.addInput(i));
        program.run();
    }

    private String getOutput() {
        StringBuilder stringBuilder = new StringBuilder();
        while (program.hasOutput()) {
            stringBuilder.append(Character.toString((int) program.getOutput()));
        }
        return stringBuilder.toString();
    }

    private List<Long> getOutputAsLongs() {
        List<Long> longs = new ArrayList<>();
        while (program.hasOutput()) {
            longs.add(program.getOutput());
        }
        return longs;
    }

    int walkAllScaffoldsAndCollectDust() {
        // movement functions
        String mfA = "L,12,L,12,L,6,L,6";
        String mfB = "R,8,R,4,L,12";
        String mfC = "L,12,L,6,R,12,R,8";
        // main movement routine
        String mainMovementRoutine = "A,B,A,C,B,A,C,B,A,C";
        // (L12L12L6L6)(R8R4L12)(L12L12L6L6)(L12L6R12R8)(R8R4L12)(L12L12L6L6)(L12L6R12R8)(R8R4L12)(L12L12L6L6)(L12L6R12R8)
        //       A         B          A           C         B          A           C         B          A           C

        program.setMemoryZeroValue(2);
        sendCommandAndReceiveOutput(mainMovementRoutine);
        sendCommandAndReceiveOutput(mfA);
        sendCommandAndReceiveOutput(mfB);
        sendCommandAndReceiveOutput(mfC);
        sendCommand("n");
//        System.out.println("output = " + getOutput());

        return getNrDustFromOutput();
    }

    private int getNrDustFromOutput() {
        List<Long> longs = getOutputAsLongs();
        StringBuilder nrDust = new StringBuilder();

        boolean markerFound = false; // 2 consecutive newlines
        for (int i = 1; i < longs.size(); i++) {
            if (!markerFound) {
                if (longs.get(i - 1) == 10 && longs.get(i) == 10) {
                    markerFound = true;
                }
            } else {
                nrDust.append(longs.get(i));
            }
        }

        return Integer.parseInt(nrDust.toString());
    }
}

