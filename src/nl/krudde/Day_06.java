package nl.krudde;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Day_06 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    static Map<String, String> mapOrbits = new HashMap<>();

    public static void main(String[] args) throws IOException {
        List<String> input = readFile(args);

        // part 1
        LocalTime start = LocalTime.now();
        System.out.println("\npart 1: ");

        input.forEach(Day_06::processOrbitRelation);

        int count = mapOrbits.keySet().stream()
                .mapToInt(orbit -> countOrbits(orbit, 0))
                .sum();
        System.out.println("count = " + count);

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        List<String> myParents = getParents("YOU");
        List<String> santaParents = getParents("SAN");
        //find unique elements and correct (-2) for ourselves (YOU/SAN)
        long transfers = Stream.concat(myParents.stream(), santaParents.stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .count() - 2;
        System.out.println("transfers = " + transfers);

        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static List<String> getParents(String orbit) {
        if (!mapOrbits.containsKey(orbit)) {
            return List.of(orbit);
        } else {
            List<String> l = new ArrayList<>(getParents(mapOrbits.get(orbit)));
            l.add(orbit);
            return l;
        }
    }

    private static int countOrbits(String orbit, int depth) {
        if (!mapOrbits.containsKey(orbit)) {
            return depth;
        } else {
            return countOrbits(mapOrbits.get(orbit), depth + 1);
        }
    }

    private static void processOrbitRelation(String line) {
        String orbitLeft = line.split("[)]")[0];
        String orbitRight = line.split("[)]")[1];
        if (!mapOrbits.containsKey(orbitRight)) {
            mapOrbits.put(orbitRight, orbitLeft);
        } else {
            throw new IllegalStateException("misorbit");
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
