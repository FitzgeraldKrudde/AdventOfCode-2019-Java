package nl.krudde;

import lombok.*;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.nio.file.Path.of;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.*;

public class Day_03 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    final static Point startPoint = new Point(0, 0);

    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        List<String> input = readFile(args);

        // part 1
        System.out.println("\npart 1: ");
        Wire wire1 = buildWire(input.get(0));
        Wire wire2 = buildWire(input.get(1));

        List<Point> intersections=
                Stream.concat(wire1.getAllPoints().stream().distinct(), wire2.getAllPoints().stream().distinct())
                        .collect(groupingBy(Function.identity(), counting()))
                        .entrySet()
                        .stream()
                        .filter(p -> p.getValue() > 1)
                        .map(Map.Entry::getKey)
                        .collect(toList());

        Point closestIntersection =
                intersections.stream()
                        .min(comparingInt(p -> calculateManhattanDistance(startPoint, p)))
                        .get();

        int closestIntersectionDistance = calculateManhattanDistance(startPoint, closestIntersection);
        System.out.println("closestIntersectionDistance = " + closestIntersectionDistance);

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();

        System.out.println("\npart 2: ");
        int shortestSumWireDistanceToIntersection = intersections.stream()
                .mapToInt(intersection -> calculateSumWireDistance(intersection, wire1, wire2))
                .min()
                .getAsInt();
        System.out.println("shortestSumWireDistanceToIntersection = " + shortestSumWireDistanceToIntersection);

        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static int calculateSumWireDistance(Point intersection, Wire wire1, Wire wire2) {
        int distance = 0;
        Iterator<Point> points = wire1.getAllPoints().iterator();
        while (points.hasNext() && !points.next().equals(intersection)) {
            distance++;
        }
        points = wire2.getAllPoints().iterator();
        while (points.hasNext() && !points.next().equals(intersection)) {
            distance++;
        }
        // add both intersections
        return distance + 2;
    }

    private static List<Point> getIntersections(Path path1, Path path2) {
        return Stream.concat(path1.getPoints().stream(), path2.getPoints().stream())
                .collect(groupingBy(Function.identity(), counting()))
                .entrySet()
                .stream()
                .filter(p -> p.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(toList());
    }

    private static Wire buildWire(String s) {
        Wire wire = new Wire();
        Arrays.stream(s.split(","))
                .forEach(wire::addDirection);
        return wire;
    }

    public static int calculateManhattanDistance(Point p1, Point p2) {
        int distanceX = Integer.max(p1.getX(), p2.getX()) - Integer.min(p1.getX(), p2.getX());
        int distanceY = Integer.max(p1.getY(), p2.getY()) - Integer.min(p1.getY(), p2.getY());

        return distanceX + distanceY;
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
        List<String> input = Files.lines(of(fileName)).collect(toList());
        System.out.println(String.format("read file: %s (#lines: %d)", fileName, input.size()));

        return input;
    }

    static class Wire {
        Point end = startPoint;
        @Getter
        List<Path> pathList = new ArrayList<>();

        public void addDirection(String direction) {
            int length = Integer.parseInt(direction.substring(1));

            Direction d = Direction.valueOf(direction.substring(0, 1));
            Point point = switch (d) {
                case R -> new Point(end.getX() + length, end.getY());
                case L -> new Point(end.getX() - length, end.getY());
                case U -> new Point(end.getX(), end.getY() - length);
                case D -> new Point(end.getX(), end.getY() + length);
            };
            pathList.add(new Path(end, point, d));
            end = point;
        }

        public List<Point> getAllPoints() {
            return getPathList().stream()
                    .flatMap(path -> path.getPoints().stream())
                    .collect(toList());
        }
    }

    @Data
    @RequiredArgsConstructor
    static class Path {
        @NonNull
        final Point start, end;
        @NonNull
        final Direction direction;
        List<Point> points = null;

        int getLength() {
            return switch (direction) {
                case D -> Math.abs(start.getY() - end.getY());
                case U -> Math.abs(end.getY() - start.getY());
                case L -> Math.abs(start.getX() - end.getX());
                case R -> Math.abs(end.getX() - start.getX());
            };
        }

        List<Point> getPoints() {
            if (points == null) {
                points = IntStream.rangeClosed(1, getLength())
                        .mapToObj(i -> start.nextPoint(direction, i))
                        .collect(toList());
            }
            return points;
        }
    }

    @Data
    @AllArgsConstructor
    static class Point implements Comparable<Point> {
        private int x;
        private int y;

        @Override
        public int compareTo(Point o) {
            Point other = o;
            if (this.y == other.y) {
                return x - other.x;
            } else {
                return y - other.y;
            }
        }

        Point nextPoint(Direction direction, int distance) {
            return switch (direction) {
                case D -> new Point(getX(), getY() + distance);
                case U -> new Point(getX(), getY() - distance);
                case R -> new Point(getX() + distance, getY());
                case L -> new Point(getX() - distance, getY());
            };
        }
    }

    enum Direction {
        R,
        L,
        U,
        D
    }
}
