package nl.krudde;

import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Data
@Builder
class IntcodeV12 {
    private double[] program;
    private int position;
    private int relativeBase;

    @Builder.Default
    private boolean halted = false;
    @Builder.Default
    boolean waitingForInput = false;
    @Builder.Default
    private Queue<Long> input = new LinkedList<>();
    @Builder.Default
    private Queue<Long> output = new LinkedList<>();
    @Builder.Default
    private boolean initialised = false;

    public void addInput(Long input) {
        this.input.add(input);
        waitingForInput = false;
    }

    public boolean hasOutput() {
        return !output.isEmpty();
    }

    public long getOutput() {
        return output.remove();
    }

    public void setMemoryZeroValue(int value) {
        program[0] = value;
    }

    public void run() {
        int opcode;
        double firstParameter, secondParameter;

        if (!initialised) {
            double[] inputProgram = program;
            // make space for 256K memory above the program
            program = new double[program.length + 256 * 1024];
            // copy the program
            System.arraycopy(inputProgram, 0, program, 0, inputProgram.length);
            initialised = true;
        }

        while (!halted && !waitingForInput) {
            opcode = getOpcode(position);
            switch (opcode) {
                case 1 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    int writePosition = getPosition(position, 3);
                    program[writePosition] = firstParameter + secondParameter;
                    position += 4;
                }
                case 2 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    int writePosition = getPosition(position, 3);
                    program[writePosition] = firstParameter * secondParameter;
                    position += 4;
                }
                case 3 -> {
                    if (input.peek() == null) {
                        waitingForInput = true;
                    } else {
                        waitingForInput = false;

                        int writePosition = getPosition(position, 1);
                        program[writePosition] = input.remove();
                        position += 2;
                    }
                }
                case 4 -> {
                    firstParameter = getParameter(position, 1);
                    output.add((long) firstParameter);
                    position += 2;
                }
                case 5 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    if (firstParameter != 0) {
                        position = (int) secondParameter;
                    } else {
                        position += 3;
                    }
                }
                case 6 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    if (firstParameter == 0) {
                        position = (int) secondParameter;
                    } else {
                        position += 3;
                    }
                }
                case 7 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    int writePosition = getPosition(position, 3);
                    program[writePosition] = firstParameter < secondParameter ? 1 : 0;
                    position += 4;
                }
                case 8 -> {
                    firstParameter = getParameter(position, 1);
                    secondParameter = getParameter(position, 2);
                    int writePosition = getPosition(position, 3);
                    program[writePosition] = firstParameter == secondParameter ? 1 : 0;
                    position += 4;
                }
                case 9 -> {
                    firstParameter = getParameter(position, 1);
                    relativeBase += firstParameter;
                    position += 2;
                }
                default -> throw new IllegalStateException("unknown opcode: " + opcode);
            }
            halted = program[position] == 99;
        }
    }

    private int getOpcode(int position) {
        return (int) (program[position] % 100);
    }

    private double getParameter(int positionInstruction, int index) {
        return program[getPosition(positionInstruction, index)];
    }

    private int getPosition(int positionInstruction, int index) {
        int instruction = (int) program[positionInstruction];
        int mode = (int) ((instruction / (10 * (int) Math.pow(10, index))) % 10);
        return switch (Mode.of(mode)) {
            case MODE_IMMEDIATE -> positionInstruction + index;
            case MODE_POSITION -> (int) program[positionInstruction + index];
            case MODE_RELATIVE -> (int) (program[positionInstruction + index] + relativeBase);
            default -> throw new IllegalStateException("unknown mode: " + mode);
        };
    }
}

public class Day_25 {

    final static String DEFAULT_FILENAME = new Object() {
    }.getClass().getEnclosingClass().getSimpleName().toLowerCase().replace("_0", "_") + ".txt";

    int runs = 0;

    public static void main(String[] args) throws IOException {
        LocalTime start = LocalTime.now();

        String input = readFile(args).get(0);

        // part 1
        System.out.println("\npart 1: ");

        double[] intCodeProgram = Arrays.stream(input.split(","))
                .mapToDouble(Double::valueOf)
                .toArray();

        IntcodeV12 intcode = new IntcodeV12.IntcodeV12Builder()
                .program(intCodeProgram)
                .build();

        // I used a combination of AI and HI (Human Intelligence)
        // HI to explore the map and pick up all the items (except the ones that killed me)
        // HI could maybe be automated but it was part of the fun..
        // and AI to determine the needed weight
        Droid droid = new Droid(intcode);

        String output = droid.goToPressureSensitiveFloorRoom();
        System.out.println("output = " + output);

        String code = droid.guessWeightAndGetCode();
        System.out.println("\ncode = " + code);

//        Scanner scanner = new Scanner(System.in);
//        String command = "";
//        while (!"exit".equals(command)) {
//            command = scanner.nextLine();
//            System.out.println("output = " + droid.sendCommand(command));
//        }

        System.out.println();

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");


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

@Data
class Droid {
    private final IntcodeV12 computer;
    private Map<Point, MapCoordinateStatus> area = new TreeMap<>();
    private final Point start = new Point(0, 0);

    int moves = 0;

    public Droid(IntcodeV12 computer) {
        super();
        this.computer = computer;
    }

    public String sendCommand(String command) {
        (command + "\n").chars().boxed()
                .forEach(i -> computer.addInput((long) i));
        computer.run();

        return getOutput();
    }

    public String goToPressureSensitiveFloorRoom() {
        String commandsToPressureSensitiveFloor = """
                east
                take antenna
                west
                north
                take weather machine
                north
                take klein bottle
                east
                take spool of cat6
                east
                north
                west
                north
                take cake
                south
                east
                east
                north
                north
                take tambourine
                south
                south
                south
                take shell
                north
                west
                south
                south
                take mug
                north
                west
                south
                south
                east""";
        return sendCommand(commandsToPressureSensitiveFloor);
    }

    public String guessWeightAndGetCode() {
        List<String> inventory = new ArrayList<>(Arrays.asList("shell", "mug", "cake", "klein bottle", "tambourine", "spool of cat6", "antenna", "weather machine"));

        String code =
                new Random().ints(0, inventory.size())
                        .mapToObj(r -> {
                            Collections.shuffle(inventory);
                            List<String> inventoryToTry = inventory.subList(0, r);

                            // drop everything
                            inventory.stream()
                                    .forEach(item -> System.out.println(sendCommand("drop " + item)));

                            // take the items in this guess
                            inventoryToTry.stream()
                                    .forEach(item -> System.out.println(sendCommand("take " + item)));

                            // try to go east
                            String outputEastCommand = sendCommand("east");
                            System.out.println("outputEastCommand = " + outputEastCommand);
                            boolean passed = !outputEastCommand.contains("Security Checkpoint");
                            if (passed) {
                                System.out.println("inventoryToTry = " + inventoryToTry);
                            }
                            return outputEastCommand;
                        })
                        .filter(output->!output.contains("Security Checkpoint"))
                        .map(output -> output.split("[0-9]{6,10}")[1])
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("unreachable form infinite IntStream.."));

        return code;
    }

    private String getOutput() {
        StringBuilder stringBuilder = new StringBuilder();
        while (computer.hasOutput()) {
            stringBuilder.append(Character.toString((int) computer.getOutput()));
        }
        return stringBuilder.toString();
    }
}
