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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static nl.krudde.MapCoordinateStatus.OPEN;
import static nl.krudde.MapCoordinateStatus.WALL;

@Data
@Builder
class IntcodeV7 {
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

public class Day_15 {

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

        IntcodeV7 intcode = new IntcodeV7.IntcodeV7Builder()
                .program(intCodeProgram)
                .build();

        RepairDroid repairDroid = new RepairDroid(intcode);
        repairDroid.exploreArea();
        int shortestPath = repairDroid.calculateShortestPathToOxygenLocation();
        System.out.println("shortestPath = " + shortestPath);

        System.out.println();

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        int minutesToFillShipWithOxygen = repairDroid.calculateMinutesToFillShipWithOxygen();
        System.out.println("minutesToFillShipWithOxygen = " + minutesToFillShipWithOxygen);

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

enum Move {
    N,
    S,
    W,
    E;

    public int getValue() {
        return switch (this) {
            case N -> 1;
            case S -> 2;
            case W -> 3;
            case E -> 4;
        };
    }

    public Move getOppositeMove() {
        return switch (this) {
            case N -> S;
            case S -> N;
            case W -> E;
            case E -> W;
        };
    }
}

enum RepairDroidStatus {
    WALL,
    MOVED,
    OXYGEN_LOCATION;

    static RepairDroidStatus of(int statuscode) {
        return switch (statuscode) {
            case 0 -> WALL;
            case 1 -> MOVED;
            case 2 -> OXYGEN_LOCATION;
            default -> throw new IllegalStateException("unknown droidrepair statuscode");
        };
    }
}

@AllArgsConstructor
enum MapCoordinateStatus {
    WALL('#'),
    OPEN('.');

    @Getter
    private final char printCharacter;
}

@Data
class RepairDroid {
    private IntcodeV7 intcodeComputer;

    Map<Point, MapCoordinateStatus> area = new TreeMap<>();
    Point start = new Point(0, 0);

    int moves = 0;

    private Point oxygenLocation;
    private Point droidLocation = new Point(0, 0);

    RepairDroid(IntcodeV7 intcodeComputer) {
        this.intcodeComputer = intcodeComputer;
    }

    public void exploreArea() {
        area.put(start, OPEN);
        exploreArea(start);
        print();
        System.out.println("moves = " + moves);
    }

    private void exploreArea(Point currentPoint) {
        // get unexplored square around us
        movesToUnexploredAreas(currentPoint)
                .stream()
                .forEach(move -> doMove(currentPoint, move, false));

    }

    private void doMove(Point point, Move move, boolean movingBack) {
        moves++;
        intcodeComputer.addInput(move.getValue());
        intcodeComputer.run();
        RepairDroidStatus status = RepairDroidStatus.of(intcodeComputer.getOutput());
        Point nextPoint = point.nextPoint(move);
//        System.out.println("point = " + point + " move = " + move + " status = " + status);
        switch (status) {
            case MOVED -> {
                area.put(nextPoint, OPEN);
                if (!movingBack) {
                    exploreArea(nextPoint);
                    //move back
                    doMove(nextPoint, move.getOppositeMove(), true);
                }
            }
            case WALL -> {
                area.put(nextPoint, WALL);
            }
            case OXYGEN_LOCATION -> {
                area.put(nextPoint, OPEN);
                oxygenLocation = nextPoint;
                System.out.println("oxygen location found@" + oxygenLocation);
                exploreArea(nextPoint);
                //move back
                doMove(nextPoint, move.getOppositeMove(), true);
            }
        }
    }

    private List<Move> movesToUnexploredAreas(Point point) {
        List<Move> moves = Arrays.stream(Move.values())
                .filter(move -> isUnexploredArea(point.nextPoint(move)))
                .collect(Collectors.toList());
        return moves;
    }

    private boolean isUnexploredArea(Point point) {
        return !area.containsKey(point);
    }

    private void print() {
        System.out.println("\nx: " + minX() + "->" + maxX() + " y: " + minY() + "->" + maxY() + "\n");

        IntStream.rangeClosed(minY(), maxY())
                .forEach(y -> {
                    IntStream.rangeClosed(minX(), maxX())
                            .forEach(x -> {
                                Point p = new Point(x, y);
                                if (p.equals(start)) {
                                    System.out.print('S');
                                } else {
                                    if (p.equals(oxygenLocation)) {
                                        System.out.print('O');
                                    } else if (area.containsKey(p)) {
                                        System.out.print(area.get(p).getPrintCharacter());
                                    } else {
                                        System.out.print(' ');
                                    }
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

    public int calculateShortestPathToOxygenLocation() {
        return calculateShortestPath(start, oxygenLocation);
    }

    private int calculateShortestPath(Point src, Point dst) {
        // use the Dijkstra algorithm

        // create a set with unvisited squares
        Set<Point> unvisitedPoints = new HashSet<>(area.keySet());

        // Map with points and (minimum) distance, initially "infinite"
        Map<Point, Integer> mapPointWithDistance = unvisitedPoints.stream()
                .collect(toMap(point -> point, point -> Integer.MAX_VALUE));
        // set the distance for the destination to 0
        mapPointWithDistance.put(dst, 0);

        // start with destination
        Point currentPoint = dst;
        boolean reachedSource = false;

        while (!unvisitedPoints.isEmpty() && !reachedSource) {
            List<Point> freeNeighbours = getOpenNeighbourPoints(currentPoint).stream()
                    .filter(point -> unvisitedPoints.contains(point))
                    .collect(toList());

            // update the distance to these neighbours if closer through this node
            int currentDistanceToNeighbour = mapPointWithDistance.get(currentPoint) + 1;
            freeNeighbours.stream()
                    .forEach(fn -> {
                        if (currentDistanceToNeighbour < mapPointWithDistance.get(fn)) {
                            mapPointWithDistance.put(fn, currentDistanceToNeighbour);
                        }
                    });
            // remove current point from unvisited set
            unvisitedPoints.remove(currentPoint);

            // check if we are done
            if (currentPoint.equals(src)) {
                reachedSource = true;
            } else {
                // set next best point
                currentPoint = unvisitedPoints.stream()
                        .min(Comparator.comparingInt(mapPointWithDistance::get))
                        .orElse(null);
            }
        }
        if (!reachedSource && unvisitedPoints.isEmpty()) {
            // unreachable
            return Integer.MAX_VALUE;
        }

        return mapPointWithDistance.get(src);
    }

    private List<Point> getOpenNeighbourPoints(Point point) {
        return Arrays.stream(Move.values())
                .filter(move -> isOpenArea(point.nextPoint(move)))
                .map(point::nextPoint)
                .collect(Collectors.toList());
    }

    private boolean isOpenArea(Point point) {
        return area.containsKey(point) && area.get(point).equals(OPEN);
    }

    public int calculateMinutesToFillShipWithOxygen() {
        return area.keySet().parallelStream()
                .filter(point -> area.get(point).equals(OPEN))
                .mapToInt(point -> calculateShortestPath(oxygenLocation, point))
                .max()
                .orElseThrow(() -> new IllegalStateException("no max"));
    }
}


