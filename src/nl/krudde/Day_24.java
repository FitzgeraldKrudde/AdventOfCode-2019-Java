package nl.krudde;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class Day_24 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        List<String> input = readFile(args);

        // part 1
        LocalTime start = LocalTime.now();
        System.out.println("\npart 1: ");

        Grid grid = new Grid();
        grid.readInput(input);
        grid.print();

        Set<String> previousAreas = new HashSet<>();
        int minute = 0;
        while (previousAreas.add(Arrays.deepToString(grid.getArea()))) {
            grid = grid.doMinute();
            minute++;
            System.out.println("\nafter " + minute + " minute");
            grid.print();
        }
        long biodiversityRating = grid.calculateBiodiversityRating();
        System.out.println("\nbiodiversityRating = " + biodiversityRating);

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        grid = new Grid();
        grid.readInput(input);
        // fix middle tile
        grid.clearMiddleTile();
        List<Grid> grids = new ArrayList<>();
        // create Arraylist with 2 empty grids above and below
        grids.add(new Grid());
        grids.add(new Grid());
        grids.add(grid);
        grids.add(new Grid());
        grids.add(new Grid());

        int numberOfMinutes = 200;
        for (minute = 0; minute < numberOfMinutes; minute++) {
            // new list of grids
            List<Grid> newGrids = new ArrayList<>();
            for (int i = 1; i < grids.size() - 1; i++) {
                newGrids.add(grids.get(i).doMinuteWithRecursion(grids.get(i - 1), grids.get(i + 1)));
            }
            // create new, empty grids above and below
            newGrids.add(0, new Grid());
            newGrids.add(0, new Grid());
            newGrids.add(new Grid());
            newGrids.add(new Grid());
            grids = newGrids;
        }

        int totalNumberOfBugs = grids.stream()
                .mapToInt(Grid::numberOfBugs)
                .sum();
        System.out.println("totalNumberOfBugs = " + totalNumberOfBugs);

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

@NoArgsConstructor
@AllArgsConstructor
@Data
class Grid {
    final private static int SIZE = 5;
    final private static int MIDDLE_COORDINATE = SIZE / 2;
    private char[][] area = new char[SIZE][SIZE];

    public void readInput(List<String> input) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                area[x][y] = input.get(y).charAt(x);
            }
        }
    }

    public void print() {
        System.out.println();
        for (int y = 0; y < area.length; y++) {
            for (int x = 0; x < area[0].length; x++) {
                System.out.print(area[x][y]);
            }
            System.out.println();
        }
    }

    public Grid doMinute() {
        char[][] areaNew = new char[SIZE][SIZE];
        for (int y = 0; y < area[0].length; y++) {
            for (int x = 0; x < area.length; x++) {
                int bugsAround = bugsAround(x, y);
                if (area[x][y] == '#') {
                    if (bugsAround == 1) {
                        areaNew[x][y] = '#';
                    } else {
                        areaNew[x][y] = '.';
                    }
                }
                if (area[x][y] == '.') {
                    if (bugsAround == 1 || bugsAround == 2) {
                        areaNew[x][y] = '#';
                    } else {
                        areaNew[x][y] = '.';
                    }
                }
            }
        }
        return new Grid(areaNew);
    }

    private int bugsAround(int x, int y) {
        int bugs = 0;

        bugs += x > 0 && area[x - 1][y] == '#' ? 1 : 0;
        bugs += x < area[y].length - 1 && area[x + 1][y] == '#' ? 1 : 0;
        bugs += y > 0 && area[x][y - 1] == '#' ? 1 : 0;
        bugs += y < area[x].length - 1 && area[x][y + 1] == '#' ? 1 : 0;

        return bugs;
    }

    private int bugsWithRecursion(int x, int y, Grid gridBelow, Grid gridabove) {
        // bugs around on this level
        int bugsAround = bugsAround(x, y);

        // layer above
        bugsAround +=
                switch (x) {
                    case 0 -> gridabove.getArea()[MIDDLE_COORDINATE - 1][MIDDLE_COORDINATE] == '#' ? 1 : 0;
                    case SIZE - 1 -> gridabove.getArea()[MIDDLE_COORDINATE + 1][MIDDLE_COORDINATE] == '#' ? 1 : 0;
                    default -> 0;
                };
        bugsAround +=
                switch (y) {
                    case 0 -> gridabove.getArea()[MIDDLE_COORDINATE][MIDDLE_COORDINATE - 1] == '#' ? 1 : 0;
                    case SIZE - 1 -> gridabove.getArea()[MIDDLE_COORDINATE][MIDDLE_COORDINATE + 1] == '#' ? 1 : 0;
                    default -> 0;
                };

        // layer below
        if (x == MIDDLE_COORDINATE && y == MIDDLE_COORDINATE - 1) {
            bugsAround += gridBelow.bugsTopSide();
        }
        if (x == MIDDLE_COORDINATE && y == MIDDLE_COORDINATE + 1) {
            bugsAround += gridBelow.bugsBottomSide();
        }
        if (x == MIDDLE_COORDINATE - 1 && y == MIDDLE_COORDINATE) {
            bugsAround += gridBelow.bugsLeftSide();
        }
        if (x == MIDDLE_COORDINATE + 1 && y == MIDDLE_COORDINATE) {
            bugsAround += gridBelow.bugsRightSide();
        }

        return bugsAround;
    }

    public Grid doMinuteWithRecursion(Grid gridBelow, Grid gridabove) {
        char[][] areaNew = new char[SIZE][SIZE];
        for (int y = 0; y < area[0].length; y++) {
            for (int x = 0; x < area.length; x++) {
                // skip centre
                if (x == MIDDLE_COORDINATE && y == MIDDLE_COORDINATE) {
                    continue;
                }
                int bugsAround = bugsWithRecursion(x, y, gridBelow, gridabove);
                if (area[x][y] == '#') {
                    if (bugsAround == 1) {
                        areaNew[x][y] = '#';
                    } else {
                        areaNew[x][y] = '.';
                    }
                } else {
                    if (bugsAround == 1 || bugsAround == 2) {
                        areaNew[x][y] = '#';
                    } else {
                        areaNew[x][y] = '.';
                    }
                }
            }
        }
        return new Grid(areaNew);
    }

    public long calculateBiodiversityRating() {
        long biodiversityRating = 0;
        int i = 0;
        for (int y = 0; y < area.length; y++) {
            for (int x = 0; x < area[0].length; x++) {
                if (area[x][y] == '#') {
                    biodiversityRating += Math.pow(2, i);
                }
                i++;
            }
        }
        return biodiversityRating;
    }

    public int numberOfBugs() {
        int bugs = 0;
        for (int y = 0; y < area.length; y++) {
            for (int x = 0; x < area[0].length; x++) {
                if (area[x][y] == '#') {
                    bugs++;
                }
            }
        }
        return bugs;
    }

    public void clearMiddleTile() {
        area[SIZE / 2][SIZE / 2] = '.';
    }

    public int bugsLeftSide() {
        int bugs = 0;
        for (int y = 0; y < SIZE; y++) {
            bugs += area[0][y] == '#' ? 1 : 0;
        }
        return bugs;
    }

    public int bugsRightSide() {
        int bugs = 0;
        for (int y = 0; y < SIZE; y++) {
            bugs += area[SIZE - 1][y] == '#' ? 1 : 0;
        }
        return bugs;
    }

    public int bugsTopSide() {
        int bugs = 0;
        for (int x = 0; x < SIZE; x++) {
            bugs += area[x][0] == '#' ? 1 : 0;
        }
        return bugs;
    }

    public int bugsBottomSide() {
        int bugs = 0;
        for (int x = 0; x < SIZE; x++) {
            bugs += area[x][SIZE - 1] == '#' ? 1 : 0;
        }
        return bugs;
    }
}
