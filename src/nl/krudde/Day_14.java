package nl.krudde;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class Day_14 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    public static void main(String[] args) throws IOException {
        List<String> input = readFile(args);

        // part 1
        LocalTime start = LocalTime.now();
        System.out.println("\npart 1: ");

        List<Reaction> reactions = readReactionsFromInput(input);
        NanoFactory nanoFactory = new NanoFactory(reactions);
        long nrRequiredORE = nanoFactory.calculateAmountOfOre(1);
        System.out.println("nrRequiredORE = " + nrRequiredORE);

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        long maxFuel = calculateMaxFuelForORE(nanoFactory, 0, 1000000000000L);
        System.out.println("maxFuel = " + maxFuel);

        finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());
    }

    private static long calculateMaxFuelForORE(NanoFactory nanoFactory, long lowerBoundFuel, long upperBoundFuel) {
        long ONE_TRILLION = 1000000000000L;
        nanoFactory.reset();

        if (lowerBoundFuel == upperBoundFuel) {
            return lowerBoundFuel;
        }

        if (lowerBoundFuel + 1 == upperBoundFuel) {
            long nrRequiredORE = nanoFactory.calculateAmountOfOre(upperBoundFuel);
            if (nrRequiredORE > ONE_TRILLION) {
                return lowerBoundFuel;
            } else {
                return upperBoundFuel;
            }
        }

        long currentFuel = (lowerBoundFuel + upperBoundFuel) / 2;
        long nrRequiredORE = nanoFactory.calculateAmountOfOre(currentFuel);
        if (nrRequiredORE > ONE_TRILLION) {
            return calculateMaxFuelForORE(nanoFactory, lowerBoundFuel, currentFuel);
        } else {
            return calculateMaxFuelForORE(nanoFactory, currentFuel, upperBoundFuel);
        }
    }

    private static List<Reaction> readReactionsFromInput(List<String> input) {
        return input.stream()
                .filter(l -> l.trim().length() > 0)
                .map(line -> {
                    List<String> parameters =
                            Arrays.asList(line.replaceAll("[,>=]", "")
                                    .replaceAll(" +", " ")
                                    .split(" "));
                    List<Chemical> chemicals = findChemicals(parameters);
                    Chemical output = chemicals.get(chemicals.size() - 1);
                    chemicals.remove(chemicals.size() - 1);
                    Reaction reaction = new Reaction(chemicals, output);
                    return reaction;
                })
                .collect(toList());
    }


    public static List<Chemical> findChemicals(List<String> list) {
        List<Chemical> chemicals = new LinkedList<>();
        // quite a dirty stream-ish hack to collect pairs from a stream..
        list.stream().reduce((a, b) -> {
            try {
                chemicals.add(new Chemical(Integer.parseInt(a), b));
            } catch (NumberFormatException nfe) {
            }
            return b;
        });
        return chemicals;
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
class Chemical {
    private long amount;
    private String name;
}

@AllArgsConstructor
@Data
class Reaction {
    private List<Chemical> input;
    private Chemical output;
}

@AllArgsConstructor
@Data
class NanoFactory {
    private List<Reaction> reactions;
    private Map<String, Long> chemicalsAvailable = new TreeMap<>();
    private long amountORENeeded;

    NanoFactory(List<Reaction> reactions) {
        this.reactions = reactions;
    }

    public void reset() {
        chemicalsAvailable = new TreeMap<>();
    }

    public long calculateAmountOfOre(long amountFuel) {
        return calculateRequiredChemical(0, "FUEL", amountFuel);
    }

    private long requestFromSupply(String name, long amountRequested) {
        if (!chemicalsAvailable.containsKey(name)) {
            return 0;
        } else {
            long amountAvailable = chemicalsAvailable.get(name);
            if (amountRequested > amountAvailable) {
                chemicalsAvailable.remove(name);
                return amountAvailable;
            } else {
                chemicalsAvailable.put(name, amountAvailable - amountRequested);
                return amountRequested;
            }
        }
    }

    private void putInSupply(String name, long amount) {
        if (chemicalsAvailable.containsKey(name)) {
            chemicalsAvailable.put(name, chemicalsAvailable.get(name) + amount);
        } else {
            chemicalsAvailable.put(name, amount);
        }
    }

    private long calculateRequiredChemical(long currentAmountORENeeded, final String name, final long amount) {
        if ("ORE".equals(name)) {
            return currentAmountORENeeded + amount;
        }

        // check supply
        long amountNeeded = amount - requestFromSupply(name, amount);

        long sumRequiredOREForInput = 0;
        if (amountNeeded > 0) {
            Reaction reaction = reactions.stream()
                    .filter(r -> r.getOutput().getName().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("no reaction"));

            long nrRequiredUnitsOutputChemical;
            // calculate waste for the output chemical
            if (reaction.getOutput().getAmount() == 1) {
                nrRequiredUnitsOutputChemical = amountNeeded;
            } else {
                if (amountNeeded > reaction.getOutput().getAmount()) {
                    long waste = (reaction.getOutput().getAmount() - (amountNeeded % reaction.getOutput().getAmount())) % reaction.getOutput().getAmount();
                    if (waste > 0) {
                        // add waste to supply
                        putInSupply(name, waste);
                        nrRequiredUnitsOutputChemical = amountNeeded / reaction.getOutput().getAmount() + 1;
                    } else {
                        nrRequiredUnitsOutputChemical = amountNeeded / reaction.getOutput().getAmount();
                    }
                } else {
                    nrRequiredUnitsOutputChemical = 1;
                    // check for waste
                    if (amountNeeded < reaction.getOutput().getAmount()) {
                        long waste = reaction.getOutput().getAmount() - amountNeeded;
                        // add waste to supply
                        putInSupply(name, waste);
                    }
                }
            }

            long outputUnits = nrRequiredUnitsOutputChemical;
            // calculate required inputs
            sumRequiredOREForInput = reactions.stream()
                    .filter(r -> r.getOutput().getName().equals(name))
                    .flatMap(r -> r.getInput().stream())
                    .mapToLong(input -> calculateRequiredChemical(currentAmountORENeeded, input.getName(), outputUnits * input.getAmount()))
                    .sum();
        }
        return currentAmountORENeeded + sumRequiredOREForInput;
    }
}

