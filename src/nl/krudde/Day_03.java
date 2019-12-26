package nl.krudde;

import lombok.*;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.nio.file.Path.of;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.*;

public class Day_03 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public final static Point STARTPOINT = new Point(0, 0);

    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        List<String> input = readFile(args);

        // part 1
        System.out.println("\npart 1: ");
        Wire wire1 = buildWire(input.get(0));
        Wire wire2 = buildWire(input.get(1));

        List<Point> intersections =
                Stream.concat(wire1.getAllPoints().stream().distinct(), wire2.getAllPoints().stream().distinct())
                        .collect(groupingBy(Function.identity(), counting()))
                        .entrySet()
                        .stream()
                        .filter(p -> p.getValue() > 1)
                        .map(Map.Entry::getKey)
                        .collect(toList());

        Point closestIntersection =
                intersections.stream()
                        .min(comparingInt(p -> calculateManhattanDistance(STARTPOINT, p)))
                        .orElseThrow(() -> new IllegalStateException("no intersection found"));

        int closestIntersectionDistance = calculateManhattanDistance(STARTPOINT, closestIntersection);
        System.out.println("closestIntersectionDistance = " + closestIntersectionDistance);

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();

        System.out.println("\npart 2: ");
        int shortestSumWireDistanceToIntersection = intersections.parallelStream()
                .mapToInt(intersection -> calculateSumWireDistance(intersection, wire1, wire2))
                .min()
                .orElseThrow(() -> new IllegalStateException("no values"));
        System.out.println("shortestSumWireDistanceToIntersection = " + shortestSumWireDistanceToIntersection);

        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static int calculateSumWireDistance(Point intersection, Wire wire1, Wire wire2) {
        return wire1.getAllPoints().indexOf(intersection) + wire2.getAllPoints().indexOf(intersection) + 2;
    }

    private static Wire buildWire(String s) {
        Wire wire = new Wire(STARTPOINT);
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
}

enum PaintRobotDirection {
    LEFT90,
    RIGHT90;

    static PaintRobotDirection of(int i) {
        return switch (i) {
            case 0 -> LEFT90;
            case 1 -> RIGHT90;
            default -> throw new IllegalArgumentException("unknown direction: " + i);
        };
    }
}

enum Direction {
    R,
    L,
    U,
    D;

    Direction nextDirection(PaintRobotDirection direction) {
        switch (this) {
            case D -> {
                return switch (direction) {
                    case LEFT90 -> R;
                    case RIGHT90 -> L;
                };
            }
            case U -> {
                return switch (direction) {
                    case LEFT90 -> L;
                    case RIGHT90 -> R;
                };
            }
            case R -> {
                return switch (direction) {
                    case LEFT90 -> U;
                    case RIGHT90 -> D;
                };
            }
            case L -> {
                return switch (direction) {
                    case LEFT90 -> D;
                    case RIGHT90 -> U;
                };
            }
        }
        throw new IllegalStateException("unknown state: current direction: " + direction + " intended direction: " + direction);
    }
}

class Wire {
    Point end;
    @Getter
    List<Path> pathList = new ArrayList<>();

    Wire(Point start) {
        end = start;
    }

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
class Path {
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
class Point implements Comparable<Point> {
    protected int x;
    protected int y;

    Point(){
    }

    @Override
    public int compareTo(Point o) {
        if (this.y == o.y) {
            return x - o.x;
        } else {
            return y - o.y;
        }
    }

    Point nextPoint(Direction direction) {
        return nextPoint(direction,1);
    }

    Point nextPoint(Direction direction, int distance) {
        return switch (direction) {
            case D -> new Point(getX(), getY() + distance);
            case U -> new Point(getX(), getY() - distance);
            case R -> new Point(getX() + distance, getY());
            case L -> new Point(getX() - distance, getY());
        };
    }

    Point nextPoint(Move move) {
        return switch (move) {
            case N -> new Point(getX(), getY() - 1);
            case S -> new Point(getX(), getY() + 1);
            case W -> new Point(getX() - 1, getY());
            case E -> new Point(getX() + 1, getY());
        };
    }

    Point nextPoint(Point delta) {
        return new Point(x + delta.getX(), y + delta.getY());
    }
}
