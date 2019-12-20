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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static nl.krudde.VaultFieltType.OPEN;
import static nl.krudde.VaultFieltType.WALL;

@Data
@Builder

public class Day_18 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        List<String> input = readFile(args);

        // part 1
        System.out.println("\npart 1: ");

        UndergroundVault undergroundVault = new UndergroundVault(input);
        long nrStepsToCollectAllKeys = undergroundVault.collectAllKeys();
        System.out.println("nrStepsToCollectAllKeys = " + nrStepsToCollectAllKeys);

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

@AllArgsConstructor
enum VaultFieltType {
    WALL('#'),
    OPEN('.'),
    ENTRANCE('@'),
    ;

    @Getter
    private final char printCharacter;

    static VaultFieltType of(int i) {
        return Arrays.stream(values())
                .filter(c -> c.printCharacter == i)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("unknown vault field"));
    }
}

@Data
class UndergroundVault {
    //    private final static int UNREACHABLE = Integer.MAX_VALUE;
    private final static int UNREACHABLE = 9999;
    Map<Point, VaultFieltType> area = new TreeMap<>();
    private Map<Point, Character> keys = new TreeMap<>();
    private Map<Point, Character> doors = new TreeMap<>();
    private Map<String, Integer> cache = new TreeMap<>();
    private Point entrance;

    UndergroundVault(List<String> input) {
        AtomicInteger x = new AtomicInteger(0);
        AtomicInteger y = new AtomicInteger(0);
        input.stream()
                .forEach(line -> {
                    x.set(0);
                    line.chars()
                            .forEach(c -> {
                                Point point = new Point(x.get(), y.get());
                                if (c == VaultFieltType.ENTRANCE.getPrintCharacter()) {
                                    entrance = point;
                                    area.put(point, OPEN);
                                } else {
                                    if (Character.isAlphabetic(c)) {
                                        if (Character.isUpperCase(c)) {
                                            doors.put(point, (char) c);
                                        } else {
                                            keys.put(point, (char) c);
                                        }
                                        area.put(point, OPEN);
                                    } else {
                                        area.put(point, VaultFieltType.of(c));
                                    }
                                }
                                x.incrementAndGet();
                            });
                    y.incrementAndGet();
                });
        print();
    }

    private void print() {
        System.out.println("\nx: " + minX() + "->" + maxX() + " y: " + minY() + "->" + maxY() + "\n");

        IntStream.rangeClosed(minY(), maxY())
                .forEach(y -> {
                    IntStream.rangeClosed(minX(), maxX())
                            .forEach(x -> {
                                assert area.keySet().contains(new Point(x, y));
                                Point p = new Point(x, y);
                                if (p.equals(entrance)) {
                                    System.out.print(VaultFieltType.ENTRANCE.getPrintCharacter());
                                } else if (doors.keySet().contains(p)) {
                                    System.out.print(doors.get(p));
                                } else if (keys.containsKey(p)) {
                                    System.out.print(keys.get(p));
                                } else {
                                    System.out.print(area.get(p).getPrintCharacter());
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

    private int calculateShortestPath(Point src, Point dst, Map<Point, Character> doors, Map<Point, Character> keys) {
        return calculateDistanceMap(src, doors, keys).get(dst);
    }

    private Map<Point, Integer> calculateDistanceMap(Point src, Map<Point, Character> doors, Map<Point, Character> keys) {
        // use the Dijkstra algorithm

        // create a set with unvisited squares
        Set<Point> unvisitedPoints = area.keySet().stream()
                .filter(point -> !isWall(point))
                .filter(point -> !doors.containsKey(point))
                .collect(Collectors.toSet());

        // Map with points and (minimum) distance, initially unreachable
        Map<Point, Integer> mapPointWithDistance = unvisitedPoints.stream()
                .collect(toMap(point -> point, point -> UNREACHABLE));
        // set the distance for the source to 0
        mapPointWithDistance.put(src, 0);

        // start with source
        Point currentPoint = src;

        while (!unvisitedPoints.isEmpty()) {
            List<Point> freeNeighbours = getOpenNeighbourPoints(currentPoint, doors).stream()
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

            // set next best point
            currentPoint = unvisitedPoints.stream()
                    .min(Comparator.comparingInt(mapPointWithDistance::get))
                    .orElse(null);
        }

//        System.out.println("src+dst+distance = " + src + "+" + dst + "=" + mapPointWithDistance.get(src));
        return mapPointWithDistance;
    }

    private List<Point> getOpenNeighbourPoints(Point point, Map<Point, Character> doors) {
        List<Point> points = Arrays.stream(Move.values())
                .map(point::nextPoint)
                .filter(p -> !isWall(p))
                .filter(p -> !doors.keySet().contains(p))
                .collect(Collectors.toList());

        return points;
    }

    private boolean isWall(Point point) {
        return area.containsKey(point) && area.get(point).equals(WALL);
    }

    public long collectAllKeys() {
        return collectAllKeys(entrance, doors, keys);
    }

    public long collectAllKeys(Point point, Map<Point, Character> doors, Map<Point, Character> keys) {
        if (keys.size() == 0) {
            // apparantly we got all keys
            return 0;
        }

        // check our cache (order of keys and doors does not matter)
        String cacheKey = cacheKey(point, doors, keys);
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        Map<Point, Integer> reachableKeys = calculateDistanceMap(point, doors, keys);
        reachableKeys.entrySet().removeIf(entry -> !keys.containsKey(entry.getKey()));
        reachableKeys.entrySet().removeIf(entry -> entry.getValue() == UNREACHABLE);

        // try all reachable keys
        long nrSteps = reachableKeys.entrySet().parallelStream()
                .mapToLong(entry -> {
                    Map<Point, Character> newDoors = new HashMap<>(doors);
                    newDoors.values().remove(Character.toUpperCase(keys.get(entry.getKey())));
                    Map<Point, Character> newKeys = new HashMap<>(keys);
                    newKeys.remove(entry.getKey());
                    return entry.getValue() + collectAllKeys(entry.getKey(), newDoors, newKeys);
                })
                .min()
                .orElse(9999);

        cache.put(cacheKey, (int) nrSteps);
        System.out.println("#cache=" + cache.size() + " " + point + " #D=" + doors.size() + " #K=" + keys.size());
        return nrSteps;
    }

    private String cacheKey(Point point, Map<Point, Character> doors, Map<Point, Character> keys) {
        return point.toString() + doors.toString() + keys.toString();
    }
}


