package nl.krudde;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class Day_08 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        List<String> input = readFile(args);

        // part 1
        LocalTime start = LocalTime.now();
        System.out.println("\npart 1: ");

        int wide = 25;
        int tall = 6;
        int[][] encodedImage;

        int nrDigitsInInput = input.get(0).length();
        int nrLayers = nrDigitsInInput / (wide * tall);
        encodedImage = new int[nrLayers][wide * tall];
        IntStream.range(0, nrDigitsInInput)
                .forEach(i -> {
                    int layer = i / 150;
                    int position = i % 150;
                    encodedImage[layer][position] = Character.getNumericValue(input.get(0).charAt(i));
                });


        int fewest0Digits = Integer.MAX_VALUE;
        long multiplyNrOnesAndNrTwosForLayer = 0;
        for (int[] layer : encodedImage) {
            int[] counters = new int[3];
            for (int i : layer) {
                counters[i]++;
            }
            if (counters[0] < fewest0Digits) {
                multiplyNrOnesAndNrTwosForLayer = counters[1] * counters[2];
                fewest0Digits = counters[0];
            }
        }
        System.out.println("multiplyNrOnesAndNrTwosForLayer = " + multiplyNrOnesAndNrTwosForLayer);

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        // per pixel: find the first non-transparent pixel in the layers
        int TRANSPARANT = 2;
        int[] decodedImage = new int[wide * tall];
        for (int i = 0; i < decodedImage.length; i++) {
            for (int layer = 0; layer < nrLayers; layer++) {
                if (encodedImage[layer][i] != TRANSPARANT) {
                    decodedImage[i] = encodedImage[layer][i];
                    break;
                }
            }
        }

        // print the image/message
        for (int i = 0; i < decodedImage.length; i++) {
            if (i % wide == 0) {
                System.out.println();
            }
            if (decodedImage[i] == 0) {
                System.out.print(" ");
            } else {
                System.out.print('#');
            }
        }
        System.out.println();

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
