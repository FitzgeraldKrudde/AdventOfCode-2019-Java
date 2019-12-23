package nl.krudde;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

@Data
@Builder
class IntcodeV11 {
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

public class Day_23 {

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
        // build 50 computers
        int nrComputers = 50;
        Network network = new Network(nrComputers, intCodeProgram);

        int iteration = 0;
        while (network.getMessageForDestination255().isEmpty()) {
            network.receiveAndSendMessages();
//            System.out.println(String.format("iteration %d done, #message in network: %d", iteration, network.getNrMessages()));
            iteration++;
        }

        Message message = network.getMessageForDestination255().get();
        System.out.println("message.getY() = " + message.getY());

        System.out.println();

        LocalTime finish = LocalTime.now();
        System.out.println("duration (ms): " + Duration.between(start, finish).toMillis());

        // part 2
        start = LocalTime.now();
        System.out.println("\npart 2: ");

        network = new Network(nrComputers, intCodeProgram);
        iteration = 0;
        while (network.getFirstDuplicateYValueSentToNAT().isEmpty()) {
            network.receiveAndSendMessages();
//            System.out.println(String.format("iteration %d done, #message in network: %d", iteration, network.getNrMessages()));
            iteration++;
        }
        System.out.println("firstDuplicateYValueSentToNAT() = " + network.getFirstDuplicateYValueSentToNAT().get());


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
@AllArgsConstructor
class Message {
    private int address;
    private long x;
    private long y;
}

class Network {
    List<IntcodeV11> computers = new ArrayList<>();
    List<Message> messages = new ArrayList<>();
    private boolean receiveIdle = false;
    private boolean sendIdle = false;
    Message natMessage;
    Set<Long> sendNATYValues = new HashSet<>();
    private Optional<Long> firstDuplicateYValueSentToNAT = Optional.empty();

    Network(int nrComputers, double[] program) {
        // build computers
        IntStream.range(0, nrComputers)
                .forEach(i -> computers.add(new IntcodeV11.IntcodeV11Builder()
                        .program(program)
                        .build()));

        bootComputers();
    }

    private boolean idle() {
        return receiveIdle && sendIdle;
    }

    private void bootComputers() {
        IntStream.range(0, computers.size())
                .forEach(i -> {
                    IntcodeV11 computer = computers.get(i);
                    computer.addInput((long) i);
                    computer.addInput((long) -1);
                    computer.run();
                    while (computer.hasOutput()) {
                        // read address, x, y
                        int address = (int) computer.getOutput();
                        long x = computer.getOutput();
                        long y = computer.getOutput();
//                        System.out.println(String.format("computer: %2d address=x,y %2d=%d,%d", i, address, x, y));
                        messages.add(new Message(address, x, y));
                    }
                });
        System.out.println("booted computers = " + computers.size());
    }

    public void receiveAndSendMessages() {
        if (messages.size() == 0) {
            receiveIdle = true;
        } else {
            receiveIdle = false;
        }

        // process only current messages
        List<Message> currentMessages = new ArrayList<>(messages);
        messages.clear();

        sendMessagesAsInput(currentMessages);
        runComputersAndCollectMessages();

        if (messages.size() == 0) {
            sendIdle = true;
        } else {
            sendIdle = false;
        }

        if (idle() && natMessage != null) {
            computers.get(0).addInput(natMessage.getX());
            computers.get(0).addInput(natMessage.getY());
            if (!sendNATYValues.add(natMessage.getY())) {
                firstDuplicateYValueSentToNAT = Optional.of(natMessage.getY());
            }
        }
    }

    private void sendMessagesAsInput(List<Message> messages) {
        // send messages as input to the computers
        while (messages.size() > 0) {
            Message message = messages.remove(0);
            IntcodeV11 computer = computers.get(message.getAddress());
            computer.addInput(message.getX());
            computer.addInput(message.getY());
        }
    }

    public Optional<Message> getMessageForDestination255() {
        if (natMessage != null) {
            return Optional.of(natMessage);
        } else return Optional.empty();
    }

    private void runComputersAndCollectMessages() {
        // list of computers with messages in the network
        List<Integer> listComputerWithMessages = messages.stream()
                .map(Message::getAddress)
                .distinct()
                .collect(toList());

        IntStream.range(0, computers.size())
                .forEach(i -> {
                    IntcodeV11 computer = computers.get(i);
                    // send -1 for computers without messages
                    if (!listComputerWithMessages.contains(i)) {
                        computer.addInput((long) -1);
                    }
                    computer.run();
                    while (computer.hasOutput()) {
                        // read destination, x, y
                        int destination = (int) computer.getOutput();
                        long x = computer.getOutput();
                        long y = computer.getOutput();
//                        System.out.println(String.format("computer: %2d message %d,%d -> %d", i, x, y, destination));
                        Message message = new Message(destination, x, y);
                        if (destination == 255) {
                            natMessage = message;
                        } else {
                            messages.add(new Message(destination, x, y));
                        }
                    }
                });
    }

    public Optional<Long> getFirstDuplicateYValueSentToNAT() {
        return firstDuplicateYValueSentToNAT;
    }
}
