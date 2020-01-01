package nl.krudde;

import lombok.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;
import static nl.krudde.DonutMazeFieldType.OPEN;
import static nl.krudde.DonutMazeFieldType.WALL;

@Data
@Builder

public class Day_20 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        List<String> input = readFile(args);

        // part 1
        System.out.println("\npart 1: ");

        DonutMaze donutMaze = new DonutMaze(input);
//        System.out.println("donutMaze.portals = " + donutMaze.getPortals());

        int nrStepsShortestPath = donutMaze.nrStepsShortestPath();
        System.out.println("nrStepsShortestPath = " + nrStepsShortestPath);

        System.out.println();

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        int nrStepsShortestPathWithRecursion = donutMaze.nrStepsShortestPathWithRecursion();
        System.out.println("nrStepsShortestPathWithRecursion = " + nrStepsShortestPathWithRecursion);

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

@Data
class DonutMaze {
    //    private final static int UNREACHABLE = Integer.MAX_VALUE;
    private final static int UNREACHABLE = 999999;
    Map<Point, DonutMazeFieldType> area = new HashMap<>();
    private Point entrance;
    private Point exit;
    Map<Point, Point> portals = new HashMap<>();

    DonutMaze(List<String> input) {
        Map<Point, String> labels = new HashMap<>();

        // read maze
        for (int y = 2; y < input.size(); y++) {
            for (int x = 2; x < input.get(y).length(); x++) {
                // only process wall or open
                if (input.get(y).charAt(x) == '#' || input.get(y).charAt(x) == '.') {
                    Point point = new Point(x - 2, y - 2);
                    area.put(point, DonutMazeFieldType.of(input.get(y).charAt(x)));
                }
            }
        }

        // find vertical labels
        for (int y = 0; y < input.size() - 1; y++) {
            for (int x = 0; x < input.get(y).length(); x++) {
                if (input.get(y + 1).length() <= x || !Character.isAlphabetic(input.get(y).charAt(x)) || !Character.isAlphabetic(input.get(y + 1).charAt(x))) {
                    continue;
                }
                String label = String.valueOf(input.get(y).charAt(x)) + input.get(y + 1).charAt(x);
                if (label.trim().length() == 0) {
                    continue;
                }
                List<Point> neighbourPoints = getOpenNeighbourPoints(new Point(x - 2, y - 2));
                neighbourPoints.addAll(getOpenNeighbourPoints(new Point(x - 2, y - 1)));
                assert neighbourPoints.size() == 1;
                // check for entrance and exit
                if ("AA".equals(label)) {
                    entrance = neighbourPoints.get(0);
                } else if ("ZZ".equals(label)) {
                    exit = neighbourPoints.get(0);
                } else {
                    labels.put(neighbourPoints.get(0), label);
                }
            }
        }

        // find horizontal labels
        for (int y = 0; y < input.size(); y++) {
            for (int x = 0; x < input.get(y).length() - 1; x++) {
                if (!Character.isAlphabetic(input.get(y).charAt(x)) || !Character.isAlphabetic(input.get(y).charAt(x + 1))) {
                    continue;
                }
                String label = String.valueOf(input.get(y).charAt(x)) + input.get(y).charAt(x + 1);
                if (label.trim().length() == 0) {
                    continue;
                }
                List<Point> neighbourPoints = getOpenNeighbourPoints(new Point(x - 2, y - 2));
                neighbourPoints.addAll(getOpenNeighbourPoints(new Point(x - 1, y - 2)));
                assert neighbourPoints.size() == 1;
                if ("AA".equals(label)) {
                    entrance = neighbourPoints.get(0);
                } else if ("ZZ".equals(label)) {
                    exit = neighbourPoints.get(0);
                } else {
                    labels.put(neighbourPoints.get(0), label);
                }
            }
        }
        // transform labels Map (with 2 point entries per label)
        Map<String, List<Point>> mapLabels =
                labels.entrySet().stream()
                        .collect(groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        // add portals both ways
        mapLabels.values().stream()
                .forEach(entry -> portals.put(entry.get(0), entry.get(1)));
        mapLabels.values().stream()
                .forEach(entry -> portals.put(entry.get(1), entry.get(0)));

//        print();
    }

    private void print() {
        System.out.println("\nx: " + minX() + "->" + maxX() + " y: " + minY() + "->" + maxY() + "\n");

        IntStream.rangeClosed(minY(), maxY())
                .forEach(y -> {
                    IntStream.rangeClosed(minX(), maxX())
                            .forEach(x -> {
                                System.out.print(area.get(new Point(x, y)).getPrintCharacter());
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

    private int calculateShortestPath(Point src, Point dst) {
        return calculateDistanceMap(src).get(dst);
    }

    private int calculateShortestPathWithRecursion(Point src, Point dst) {
        // use the Dijkstra algorithm

        int highestRecursionLevelForWhichPointsHaveBeenAdded = 1;

        // upgrade area to points with recursionlevel
        Map<Point, DonutMazeFieldType> areaWithRecursion = new HashMap<>();
        area.entrySet().stream()
                .filter(entry -> isOpen(entry.getKey()))
                .forEach(entry -> areaWithRecursion.put(new PointWithRecursion(entry.getKey().getX(), entry.getKey().getY(), 0), entry.getValue()));

        // create a set with unvisited squares
        Set<PointWithRecursion> unvisitedPoints = area.keySet().stream()
                .filter(point -> !isWall(point))
                .map(point -> new PointWithRecursion(point.getX(), point.getY(), 0))
                .collect(Collectors.toSet());

        // add an extra recursion level upfront
        List<PointWithRecursion> nextRecursionLevel = area.keySet().stream()
                .filter(point -> !isWall(point))
                .map(point -> new PointWithRecursion(point.getX(), point.getY(), 1))
                .collect(Collectors.toList());
        unvisitedPoints.addAll(nextRecursionLevel);

        // Map with points and (minimum) distance, initially unreachable
        Map<PointWithRecursion, Integer> mapPointWithDistance = unvisitedPoints.stream()
                .collect(toMap(point -> point, point -> UNREACHABLE));
        PointWithRecursion srcRecursion = new PointWithRecursion(src.getX(), src.getY(), 0);
        // set the distance for the source to 0
        mapPointWithDistance.put(srcRecursion, 0);
        PointWithRecursion dstRecursion = new PointWithRecursion(dst.getX(), dst.getY(), 0);

        // start with source
        PointWithRecursion currentPoint = srcRecursion;

        boolean reachedDestination = false;

        while (!reachedDestination && !unvisitedPoints.isEmpty()) {
            List<PointWithRecursion> freeNeighbours = getOpenNeighbourPointsWithWarpsAndRecursion(currentPoint).stream()
                    .filter(point -> unvisitedPoints.contains(point))
                    .collect(toList());

            // update the distance to these neighbours if closer through this node
            int currentDistanceToNeighbour = mapPointWithDistance.get(currentPoint) + 1;
            freeNeighbours.stream()
                    .filter(fn -> currentDistanceToNeighbour < mapPointWithDistance.get(fn))
                    .forEach(fn -> mapPointWithDistance.put(fn, currentDistanceToNeighbour));

            if (currentPoint.equals(dstRecursion)) {
                reachedDestination = true;
            } else {
                // remove current point from unvisited set
                unvisitedPoints.remove(currentPoint);

                // set next best point, sort on recursionlevel instead of distance to prevent "wandering off" through the recursion levels
                currentPoint = unvisitedPoints.stream()
                        .filter(point -> mapPointWithDistance.get(point) != UNREACHABLE)
                        .min(Comparator.comparingInt(PointWithRecursion::getRecursionLevel))
                        .orElse(null);

                // check if we need to add an extra recursion level
                if (currentPoint != null && currentPoint.getRecursionLevel() == highestRecursionLevelForWhichPointsHaveBeenAdded) {
                    final int newRecursionLevel = highestRecursionLevelForWhichPointsHaveBeenAdded + 1;
                    Set<PointWithRecursion> recursiveLevel = area.keySet().stream()
                            .filter(point -> isOpen(point))
                            .filter(point -> !entrance.equals(point))
                            .filter(point -> !exit.equals(point))
                            .map(point -> new PointWithRecursion(point.getX(), point.getY(), newRecursionLevel))
                            .collect(Collectors.toSet());
                    unvisitedPoints.addAll(recursiveLevel);
                    mapPointWithDistance.putAll(recursiveLevel.stream()
                            .collect(toMap(point -> point, point -> UNREACHABLE)));
                    highestRecursionLevelForWhichPointsHaveBeenAdded = newRecursionLevel;
                    System.out.println("added new recursionlevel for level = " + newRecursionLevel + " @point = " + currentPoint + " #distancemap = " + mapPointWithDistance.size() + " #unvisitedPoints = " + unvisitedPoints.size());
                }
            }
        }

        return mapPointWithDistance.get(dstRecursion);
    }

    private Map<Point, Integer> calculateDistanceMap(Point src) {
        // use the Dijkstra algorithm

        // create a set with unvisited squares
        Set<Point> unvisitedPoints = area.keySet().stream()
                .filter(point -> !isWall(point))
                .collect(Collectors.toSet());

        // Map with points and (minimum) distance, initially unreachable
        Map<Point, Integer> mapPointWithDistance = unvisitedPoints.stream()
                .collect(toMap(point -> point, point -> UNREACHABLE));
        // set the distance for the source to 0
        mapPointWithDistance.put(src, 0);

        // start with source
        Point currentPoint = src;

        while (!unvisitedPoints.isEmpty()) {
            List<Point> freeNeighbours = getOpenNeighbourPointsWithWarps(currentPoint).stream()
                    .filter(unvisitedPoints::contains)
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

            // set next best point
            currentPoint = unvisitedPoints.stream()
                    .min(Comparator.comparingInt(mapPointWithDistance::get))
                    .orElse(null);
        }

        return mapPointWithDistance;
    }

    private List<Point> getOpenNeighbourPoints(Point point) {
        List<Point> points = Arrays.stream(Move.values())
                .map(point::nextPoint)
                .filter(this::isOpen)
                .collect(Collectors.toList());

        return points;
    }

    private List<Point> getOpenNeighbourPointsWithWarps(Point point) {
        List<Point> points = Arrays.stream(Move.values())
                .map(point::nextPoint)
                .filter(this::isOpen)
                .collect(Collectors.toList());

        if (portals.containsKey(point)) {
            points.add(portals.get(point));
        }

        return points;
    }

    private List<PointWithRecursion> getOpenNeighbourPointsWithWarpsAndRecursion(PointWithRecursion point) {
        List<PointWithRecursion> points = Arrays.stream(Move.values())
                .map(point::nextPoint)
                .filter(p -> isOpen(p))
                .collect(Collectors.toList());

        // add inner portal destination i.e. the outer portal point
        if (isInnerPortal(point)) {
            PointWithRecursion portalPoint = new PointWithRecursion(portals.get(point.toPlainPoint()), point.getRecursionLevel() + 1);
            points.add(portalPoint);
        }
        if (point.getRecursionLevel() > 0) {
            // add outer portal destination i.e. the inner portal point
            if (isOuterPortal(point)) {
                PointWithRecursion portalPoint = new PointWithRecursion(portals.get(point.toPlainPoint()), point.getRecursionLevel() - 1);
                points.add(portalPoint);
            }
        }

        return points;
    }

    private boolean isInnerPortal(PointWithRecursion point) {
        return isInnerPortal(point.toPlainPoint());
    }

    private boolean isInnerPortal(Point point) {
        return portals.containsKey(point) && (point.getX() > 0 && point.getX() < maxX() &&
                point.getY() > 0 && point.getY() < maxY());
    }

    private boolean isOuterPortal(PointWithRecursion point) {
        return portals.containsKey(point.toPlainPoint()) && (point.getX() == 0 || point.getX() == maxX() ||
                point.getY() == 0 || point.getY() == maxY());
    }

    private boolean isOpen(PointWithRecursion point) {
        return isOpen(point.toPlainPoint());
    }

    private boolean isOpen(Point point) {
        return area.containsKey(point) && area.get(point).equals(OPEN);
    }

    private boolean isWall(Point point) {
        return area.containsKey(point) && area.get(point).equals(WALL);
    }

    public int nrStepsShortestPath() {
        return calculateShortestPath(entrance, exit);
    }

    public int nrStepsShortestPathWithRecursion() {
        return calculateShortestPathWithRecursion(entrance, exit);
    }
}

@AllArgsConstructor
enum DonutMazeFieldType {
    WALL('#'),
    OPEN('.'),
    EMPTY_SPACE(' '),
    ;

    @Getter
    private final char printCharacter;

    static DonutMazeFieldType of(int i) {
        return Arrays.stream(values())
                .filter(c -> c.printCharacter == i)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("unknown donut maze field type"));
    }
}

@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
class PointWithRecursion extends Point implements Comparable<Point> {
    protected int recursionLevel = 0;

    public PointWithRecursion(int x, int y, int recursionLevel) {
        this.x = x;
        this.y = y;
        this.recursionLevel = recursionLevel;
    }

    public PointWithRecursion(Point point, int recursionLevel) {
        this(point.getX(), point.getY(), recursionLevel);
    }

    PointWithRecursion nextPoint(Move move) {
        return switch (move) {
            case N -> new PointWithRecursion(getX(), getY() - 1, recursionLevel);
            case S -> new PointWithRecursion(getX(), getY() + 1, recursionLevel);
            case W -> new PointWithRecursion(getX() - 1, getY(), recursionLevel);
            case E -> new PointWithRecursion(getX() + 1, getY(), recursionLevel);
        };
    }

    public int compareTo(PointWithRecursion o) {
        if (this.recursionLevel == o.recursionLevel) {
            return super.compareTo(o);
        } else {
            return this.recursionLevel - o.recursionLevel;
        }
    }

    public Point toPlainPoint() {
        return new Point(x, y);
    }
}
