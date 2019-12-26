package nl.krudde;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class Day_16 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        List<String> input = readFile(args);

        // part 1
        LocalTime start = LocalTime.now();
        System.out.println("\npart 1: ");

        List<Integer> listNumbers = input.get(0)
                .chars()
                .mapToObj(Character::getNumericValue)
                .collect(Collectors.toList());

        System.out.println("listNumbers = " + listNumbers);
        FlawedFrequencyTransmission flawedFrequencyTransmission = new FlawedFrequencyTransmission(new ArrayList<>(listNumbers));
        int NR_PHASES = 100;
        flawedFrequencyTransmission.phases(NR_PHASES);
        System.out.println("\nflawedFrequencyTransmission.getList() = " + flawedFrequencyTransmission.getList());
        String first8Digits = flawedFrequencyTransmission.getList().subList(0, 8).stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
        System.out.println("first8Digits = " + first8Digits);

        LocalTime finish = LocalTime.now();
        System.out.println("\nduration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        int messageOffset = Integer.parseInt(listNumbers.subList(0, 7).stream()
                .map(String::valueOf)
                .collect(Collectors.joining()));
        System.out.println("messageOffset = " + messageOffset);
        int startIndexInMessage = messageOffset % listNumbers.size();
        System.out.println("startIndexInMessage = " + startIndexInMessage);
        // create a new input list, starting from the offset
        List<Integer> listFromOffset = new ArrayList<>(listNumbers.subList(startIndexInMessage, listNumbers.size()));
        // add the needed repeated inputlists
        IntStream.range(0, 10000 - 1 - messageOffset / listNumbers.size())
                .forEach(i -> listFromOffset.addAll(listNumbers));
        flawedFrequencyTransmission = new FlawedFrequencyTransmission(new ArrayList<>(listFromOffset));
        flawedFrequencyTransmission.phases2(NR_PHASES);

        String res = flawedFrequencyTransmission.getList().subList(0, 8).stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
        System.out.println("res = " + res);


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
@Data
class FlawedFrequencyTransmission {
    final List<Integer> BASE_PATTERN = List.of(0, 1, 0, -1);

    @Getter
    private List<Integer> list;

    private List<Integer> generateRepeatingPattern(int length, int position) {
        List<Integer> pattern =
                BASE_PATTERN.stream()
                        .flatMapToInt(nr ->
                                IntStream.range(0, position)
                                        .map(i -> nr))
                        .boxed()
                        .collect(toList());

        List<Integer> repeatingPattern =
                IntStream.rangeClosed(0, length)
                        .mapToObj(i -> pattern.get(i % pattern.size()))
                        .collect(toList());

        repeatingPattern.remove(0);
        return repeatingPattern;
    }

    public void phases(int nrPhases) {
        IntStream.rangeClosed(1, nrPhases)
                .forEach(i -> phase());
    }
    public void phases2(int nrPhases) {
        IntStream.rangeClosed(1, nrPhases)
                .forEach(i -> phase2());
    }

    private void phase2() {
        IntStream.range(0, list.size() - 1)
                .map(i -> list.size() - i - 2)
                .forEach(i -> list.set(i, (list.get(i) + list.get(i + 1)) % 10));
    }

    private void phase() {
        List<Integer> result =
                IntStream.range(0, list.size())
                        .map(i -> {
                                    List<Integer> pattern = generateRepeatingPattern(list.size(), i + 1);

                                    int nr = Math.abs(IntStream.range(0, list.size())
                                            .map(j -> {
                                                int listnr = list.get(j);
                                                int patternnr = pattern.get(j);
                                                int mul = listnr * patternnr;
                                                return mul;
                                            })
                                            .sum() % 10);
                                    return nr;
                                }
                        )
                        .boxed()
                        .collect(toList());

        list = result;
    }
}
