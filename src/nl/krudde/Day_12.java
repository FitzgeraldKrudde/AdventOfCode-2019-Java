package nl.krudde;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class Day_12 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        List<String> input = readFile(args);

        // part 1
        LocalTime start = LocalTime.now();
        System.out.println("\npart 1: ");

        //<x=-7, y=17, z=-11>
        List<Moon> moons = createMoonsFromInput(input);
        List<MoonPair> moonPairs = generateMoonPairs(moons);

        int maxSteps = 1000;
        for (int i = 1; i <= maxSteps; i++) {
            moons = timeStep(moons, moonPairs);
        }

        int totalEnergy = calculateTotalEnergy((List<Moon>) moons);
        System.out.println("After " + maxSteps + " steps:");
        System.out.println("totalEnergy = " + totalEnergy + "\n");

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        moons = createMoonsFromInput(input);
        moonPairs = generateMoonPairs(moons);
        Map<String, Integer> mapXcoordinates = new HashMap<>();
        Map<String, Integer> mapYcoordinates = new HashMap<>();
        Map<String, Integer> mapZcoordinates = new HashMap<>();
        mapXcoordinates.put(getCoordinateFromMoons(moons, Coordinate.X), 0);
        mapYcoordinates.put(getCoordinateFromMoons(moons, Coordinate.Y), 0);
        mapZcoordinates.put(getCoordinateFromMoons(moons, Coordinate.Z), 0);

        boolean foundX = false;
        boolean foundY = false;
        boolean foundZ = false;

        int repeatAtStepForX = -1;
        int repeatAtStepForY = -1;
        int repeatAtStepForZ = -1;

        int step = 0;

        while (!foundX || !foundY || !foundZ) {
            moons = timeStep(moons, moonPairs);
            step++;

            String xCoordinates = getCoordinateFromMoons(moons, Coordinate.X);
            String yCoordinates = getCoordinateFromMoons(moons, Coordinate.Y);
            String zCoordinates = getCoordinateFromMoons(moons, Coordinate.Z);

            if (!foundX && mapXcoordinates.containsKey(xCoordinates)) {
                System.out.println("repeat x: " + mapXcoordinates.get(xCoordinates) + "->" + step);
                repeatAtStepForX = step;
                foundX = true;
            } else {
                mapXcoordinates.put(xCoordinates, step);
            }
            if (!foundY && mapYcoordinates.containsKey(yCoordinates)) {
                System.out.println("repeat y: " + mapYcoordinates.get(yCoordinates) + "->" + step);
                repeatAtStepForY = step;
                foundY = true;
            } else {
                mapYcoordinates.put(yCoordinates, step);
            }
            if (!foundZ && mapZcoordinates.containsKey(zCoordinates)) {
                System.out.println("repeat z: " + mapZcoordinates.get(zCoordinates) + "->" + step);
                repeatAtStepForZ = step;
                foundZ = true;
            } else {
                mapZcoordinates.put(zCoordinates, step);
            }
        }
        BigInteger nrSteps = LCM(
                LCM(BigInteger.valueOf(repeatAtStepForX), BigInteger.valueOf(repeatAtStepForY)),
                LCM(BigInteger.valueOf(repeatAtStepForY), BigInteger.valueOf(repeatAtStepForZ))
        );
        System.out.println("maxSteps = " + nrSteps);

        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static int calculateTotalEnergy(List<Moon> moons) {
        return moons.stream()
                .mapToInt(Moon::totalEnergy)
                .sum();
    }

    private static String getCoordinateFromMoons(List<Moon> moons, Coordinate coordinate) {
        int[] coordinates = moons.stream()
                .flatMapToInt(moon -> switch (coordinate) {
                    case X -> IntStream.of(moon.getPosition().getX(), moon.getVelocity().getX());
                    case Y -> IntStream.of(moon.getPosition().getY(), moon.getVelocity().getY());
                    case Z -> IntStream.of(moon.getPosition().getZ(), moon.getVelocity().getZ());
                })
                .toArray();

        return Arrays.toString(coordinates);
    }

    private static List<Moon> timeStep(List<Moon> moons, List<MoonPair> moonPairs) {
        moonPairs.stream()
                .forEach(MoonPair::applyGravity);
        moons.stream()
                .forEach(Moon::applyVelocity);

        return moons;
    }

    private static List<Moon> createMoonsFromInput(List<String> input) {
        return input.stream()
                .filter(l -> l.trim().length() > 0)
                .map(line ->
                        Arrays.stream(line.replaceAll("[ <>=xyz]", "")
                                .split(","))
                                .mapToInt(Integer::valueOf)
                                .toArray())
                .map(coordinates -> new Moon(new Point3D(coordinates[0], coordinates[1], coordinates[2])))
                .collect(toList());
    }

    private static List<MoonPair> generateMoonPairs(List<Moon> moons) {
        List<MoonPair> moonPairs = new ArrayList<>();

        // quite old school .. could not make it work with Streams :-(
        for (int i = 0; i < moons.size(); i++) {
            for (int j = 0; j < moons.size(); j++) {
                if (i == j) continue;
                MoonPair mp1 = new MoonPair(moons.get(i), moons.get(j));
                MoonPair mp2 = new MoonPair(moons.get(j), moons.get(i));
                if (!moonPairs.contains(mp1) && !moonPairs.contains(mp2)) {
                    moonPairs.add(mp1);
                }
            }
        }

        return moonPairs;
    }

    private static BigInteger LCM(BigInteger a, BigInteger b) {
        return a.multiply(b).divide(GCF(a, b));
    }

    private static BigInteger GCF(BigInteger a, BigInteger b) {
        if (b.equals(BigInteger.ZERO)) return a;
        else return (GCF(b, a.mod(b)));
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
class Point3D {
    private int x;
    private int y;
    private int z;
}

@AllArgsConstructor
@Data
class Moon {
    private Point3D position;
    private Point3D velocity;

    Moon(Point3D point) {
        position = point;
        velocity = new Point3D(0, 0, 0);
    }

    public void applyVelocity() {
        position.setX(position.getX() + velocity.getX());
        position.setY(position.getY() + velocity.getY());
        position.setZ(position.getZ() + velocity.getZ());
    }

    int totalEnergy() {
        return potentialEnergy() * kineticEnergy();
    }

    private int potentialEnergy() {
        return Math.abs(position.getX()) + Math.abs(position.getY()) + Math.abs(position.getZ());
    }

    private int kineticEnergy() {
        return Math.abs(velocity.getX()) + Math.abs(velocity.getY()) + Math.abs(velocity.getZ());
    }
}

@AllArgsConstructor
@Data
class MoonPair {
    private final Moon moon1;
    private final Moon moon2;

    public void applyGravity() {
        Point3D position1 = moon1.getPosition();
        Point3D velocity1 = moon1.getVelocity();
        Point3D position2 = moon2.getPosition();
        Point3D velocity2 = moon2.getVelocity();

        int delta;
        delta = calculateDelta(position1.getX(), position2.getX());
        velocity1.setX(velocity1.getX() + delta);
        velocity2.setX(velocity2.getX() - delta);

        delta = calculateDelta(position1.getY(), position2.getY());
        velocity1.setY(velocity1.getY() + delta);
        velocity2.setY(velocity2.getY() - delta);

        delta = calculateDelta(position1.getZ(), position2.getZ());
        velocity1.setZ(velocity1.getZ() + delta);
        velocity2.setZ(velocity2.getZ() - delta);
    }

    private int calculateDelta(int x1, int x2) {
        if (x1 == x2) return 0;

        if (x1 < x2) {
            return 1;
        }
        return -1;
    }
}

enum Coordinate {
    X,
    Y,
    Z
}
