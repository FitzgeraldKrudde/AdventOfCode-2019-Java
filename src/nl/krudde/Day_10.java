package nl.krudde;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;


public class Day_10 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        List<String> input = readFile(args);

        // part 1
        LocalTime start = LocalTime.now();
        System.out.println("\npart 1: ");

        List<Point> asteroids = parseInput(input);

        // for every 2 asteroids: calculate all points on the grid between them and check if there are any asteroids on it
        Map.Entry<Integer, List<Point>> entryNumberAsteroidsInSightAndOptimalAsteroid = asteroids.stream()
                .collect(groupingBy(asteroid -> numberOfAsteroidInDirectSight(asteroid, asteroids)))
                .entrySet()
                .stream()
                .max(Comparator.comparingInt(Map.Entry::getKey))
                .orElseThrow(() -> new IllegalStateException("no asteroid"));

        Point optimalAsteroid = entryNumberAsteroidsInSightAndOptimalAsteroid.getValue().get(0);
        System.out.println("optimalAsteroid = " + optimalAsteroid);
        int numberOfAsteroidInSight = entryNumberAsteroidsInSightAndOptimalAsteroid.getKey();
        System.out.println("numberOfAsteroidInSight = " + numberOfAsteroidInSight);

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        //remove the asteroid with the monitoring station
        asteroids.remove(optimalAsteroid);

        // bummer: part 2 really needs the actual calculated angles ...
        // group all asteroid on angle
        // sort each list (for an angle) on distance and add a sequence nr
        // then flatmap, sort all the elements on <sequencenr, angle>
        List<TriplePointAndAngleAndSequenceOnDistance> sortedTriplePointAndAngleAndSequenceOnDistanceList =
                asteroids.stream()
                        .collect(groupingBy(asteroid -> calculateAngle(optimalAsteroid, asteroid)))
                        .entrySet()
                        .stream()
                        .flatMap(entry -> {
                            //TODO improve to inline stream-ish
                            List<Point> pointList = entry.getValue().stream().sorted(Comparator
                                    .comparing(point -> calculateDistance(optimalAsteroid, point)))
                                    .collect(toList());
                            return IntStream.range(0, pointList.size())
                                    .mapToObj(i ->
                                            new TriplePointAndAngleAndSequenceOnDistance(pointList.get(i), entry.getKey(), i));
                        })
                        .sorted(Comparator
                                .comparing(TriplePointAndAngleAndSequenceOnDistance::getSequenceOnAngle)
                                .thenComparing(TriplePointAndAngleAndSequenceOnDistance::getAngle))
                        .collect(toList());

        int numberOfVaporizedAsteroidsToSkip = 199;
        Point pointVaporized200 =
                sortedTriplePointAndAngleAndSequenceOnDistanceList.stream()
                        .skip(numberOfVaporizedAsteroidsToSkip)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("no asteroid"))
                        .getPoint();
        System.out.println("pointVaporized200 = " + pointVaporized200);
        System.out.println("result: " + (pointVaporized200.getX() * 100 + pointVaporized200.getY()));

        List<String> strings = new ArrayList<>(Arrays.asList("First", "Second", "Third", "Fourth", "Fifth")); // An example list of Strings
        strings.stream() // Turn the list into a Stream
                .collect(HashMap::new, (h, o) -> h.put(h.size(), o), (h, o) -> {
                }) // Create a map of the index to the object
                .forEach((i, o) -> { // Now we can use a BiConsumer forEach!
                    System.out.println(String.format("%d => %s", i, o));
                });

        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static double calculateDistance(Point p1, Point p2) {
        // good old Pythagoras.. that was a VERY long time ago..
        int xDelta = p2.getX() - p1.getX();
        int yDelta = p2.getY() - p1.getY();

        return Math.sqrt(xDelta * xDelta + yDelta * yDelta);
    }

    private static double calculateAngle(Point p1, Point p2) {
//        Point cartesianPoint1 = new Point(0, 0);
        Point cartesianPoint2 = calculateZeroZeroBasedCartesianPoint(p1, p2);
        double rotAng = Math.abs(Math.toDegrees(Math.atan2(cartesianPoint2.getX(), -cartesianPoint2.getY())) - 180);
        return rotAng;
    }

    private static Point calculateZeroZeroBasedCartesianPoint(Point p1, Point p2) {
        int x = p2.getX() - p1.getX();
        int y = p1.getY() - p2.getY();

        return new Point(x, y);
    }


    private static int numberOfAsteroidInDirectSight(Point asteroid, List<Point> asteroids) {
        int count = (int) asteroids.stream()
                .filter(targetAsteroid -> !asteroid.equals(targetAsteroid))
                .filter(targetAsteroid -> inDirectSight(asteroids, asteroid, targetAsteroid))
                .count();

        return count;
    }

    private static boolean inDirectSight(List<Point> asteroids, Point sourceAsteroid, Point targetAsteroid) {
        List<Point> pointsBetween = calculatePointsBetweenPoint1AndPoint2(sourceAsteroid, targetAsteroid);
        // none of the points between the source and destination may have an asteroid
        boolean inDirectsight = pointsBetween.stream()
                .noneMatch(asteroids::contains);

        return inDirectsight;
    }

    private static List<Point> parseInput(List<String> input) {
        List<Point> asteroids = new ArrayList<>();
        for (int x = 0; x < input.get(0).length(); x++) {
            for (int y = 0; y < input.size(); y++) {
                if (input.get(y).charAt(x) == '#') {
                    asteroids.add(new Point(x, y));
                }
            }
        }

        return asteroids;
    }

    private static List<Point> calculatePointsBetweenPoint1AndPoint2(Point start, Point end) {
        List<Point> points = new ArrayList<>();
        // calculate the delta to reach a next point on the grid
        Point deltaPoint = calculateDeltaPoint(start, end);
        Point point = start.nextPoint(deltaPoint);

        // walk to the endpoint, collecting all intermediate points
        while (!point.equals(end)) {
            points.add(point);
            point = point.nextPoint(deltaPoint);
        }

        return points;
    }

    public static Point calculateDeltaPoint(Point start, Point end) {
        assert !start.equals(end);
        // shortcut when both points are on a horizontal or vertical line
        if (start.getX() == end.getX()) {
            if (start.getY() < end.getY()) {
                return new Point(0, 1);
            } else {
                return new Point(0, -1);
            }
        }
        if (start.getY() == end.getY()) {
            if (start.getX() < end.getX()) {
                return new Point(1, 0);
            } else {
                return new Point(-1, 0);
            }
        }

        // shortcut for diagonal lines
        int yDelta = end.getY() - start.getY();
        int xDelta = end.getX() - start.getX();

        if (xDelta == yDelta) {
            return new Point(xDelta / Math.abs(xDelta), yDelta / Math.abs(yDelta));
        }

        // simplify by using the common denominator
        BigInteger bdx = BigInteger.valueOf(xDelta);
        BigInteger bdy = BigInteger.valueOf(yDelta);
        BigInteger gcd = bdx.gcd(bdy);

        if (!gcd.equals(BigInteger.ONE)) {
            xDelta /= gcd.intValue();
            yDelta /= gcd.intValue();
        }
        return new Point(xDelta, yDelta);
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
@Data
class TriplePointAndAngleAndSequenceOnDistance {
    private Point point;
    private double angle;
    private double sequenceOnAngle;
}
