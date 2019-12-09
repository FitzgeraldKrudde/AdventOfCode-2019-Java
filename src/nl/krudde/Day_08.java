package nl.krudde;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;

@Data
class Image {
    int TRANSPARENT = 2;

    private final List<Layer> layers = new ArrayList<>();

    static Image of(String pixels, int pixelsPerLayer) {
        Image image = new Image();
        int nrLayers = pixels.length() / pixelsPerLayer;
        IntStream.range(0, nrLayers)
                .forEach(i -> {
                    Layer layer = new Layer();
                    pixels.substring(i * pixelsPerLayer, (i + 1) * pixelsPerLayer)
                            .chars()
                            .map(Character::getNumericValue)
                            .forEach(pixel -> layer.getPixels().add(pixel));
                    image.getLayers().add(layer);
                });
        return image;
    }

    Layer getLayerWithLowestNumberOfZeros() {
        return layers.parallelStream()
                .min(comparingLong(Layer::numberOfZeros))
                .orElseThrow(() -> new IllegalStateException("no value"));
    }

    Image decodeImage() {
        Image newImage = new Image();
        Layer newLayer = new Layer();

        newLayer.getPixels().addAll(
                IntStream.range(0, layers.get(0).getPixels().size())
                        .mapToObj(position -> layers.stream()
                                .map(layer -> layer.getPixels().get(position))
                                .filter(pixel -> pixel != TRANSPARENT)
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("no value"))
                        )
                        .collect(toList()));
        newImage.getLayers().add(newLayer);
        return newImage;
    }
}

@Data
@AllArgsConstructor
class Layer {
    private final List<Integer> pixels = new ArrayList<>();

    long numberOfZeros() {
        return nrDigits(0);
    }

    long numberOfOnes() {
        return nrDigits(1);
    }

    long numberOfTwos() {
        return nrDigits(2);
    }

    private long nrDigits(int digit) {
        return pixels.stream()
                .filter(pixel -> pixel == digit)
                .count();
    }
}

public class Day_08 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        List<String> input = readFile(args);

        // part 1
        LocalTime start = LocalTime.now();
        System.out.println("\npart 1: ");

        int WIDE = 25;
        int TALL = 6;

        Image encodedImage = Image.of(input.get(0), WIDE * TALL);
        Layer layerWithLowestNumberOfZeros = encodedImage.getLayerWithLowestNumberOfZeros();

        long multiplyNrOnesAndNrTwosForLayer = layerWithLowestNumberOfZeros.numberOfOnes() * layerWithLowestNumberOfZeros.numberOfTwos();
        System.out.println("multiplyNrOnesAndNrTwosForLayer = " + multiplyNrOnesAndNrTwosForLayer);

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        Image decodedImage = encodedImage.decodeImage();
        Layer layer = decodedImage.getLayers().get(0);
        // print the image/message
        for (int i = 0; i < layer.getPixels().size(); i++) {
            if (i % WIDE == 0) {
                System.out.println();
            }
            if (layer.getPixels().get(i) == 0) {
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
