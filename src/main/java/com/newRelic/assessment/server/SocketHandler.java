package com.newRelic.assessment.server;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketHandler {
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    private static final int THREADS = 5;
    private static final Path FILE_PATH = Paths.get("numbers.log");

    private final CopyOnWriteArrayList<Socket> clientSockets = new CopyOnWriteArrayList<>();
    private final Set<String> usedNumbers = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicInteger duplicateNumberCount = new AtomicInteger(0);

    private int previousDuplicates = 0;
    private int previousTotal = 0;

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        executorService = Executors.newFixedThreadPool(THREADS);
        scheduler.scheduleAtFixedRate(this::logNumber, 0, 10, TimeUnit.SECONDS);

        if (Files.exists(FILE_PATH)) {
            Files.delete(FILE_PATH);
        }
        Files.createFile(FILE_PATH);

        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                executorService.submit(() -> {
                    try {
                        handleClientInput(clientSocket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        System.out.println("Cleaning up closed sockets...");
                        try {
                            clientSocket.close();
                        } catch (IOException ignored) {}
                        clientSockets.remove(clientSocket);
                    }
                });
            }
        } catch (SocketException e) {
            if (serverSocket.isClosed()) {
                System.out.println("Server socket closed, shutting down accept loop.");
            } else {
                throw e;
            }
        }
    }

    private void handleClientInput(Socket clientSocket) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String input;
            while ((input = in.readLine()) != null) {
                if (input.equals("terminate")) {
                    shutDownSocketHandler();
                    break;
                } else if (!isValidInput(input)) {
                    clientSocket.close();
                    break;
                } else if (usedNumbers.add(input)) {
                    Files.writeString(
                            FILE_PATH,
                            input + System.lineSeparator(),
                            StandardCharsets.UTF_8,
                            StandardOpenOption.APPEND
                    );
                } else if (usedNumbers.contains(input)) {
                    duplicateNumberCount.incrementAndGet();
                }
            }
        }
    }

    private void shutDownSocketHandler() throws IOException {
        System.out.println("Terminating all client connections...");
        for (Socket socket : clientSockets) {
            socket.close();
        }
        executorService.shutdown();
        serverSocket.close();
        scheduler.shutdown();
    }

    private Boolean isValidInput(String input) {
        try {
            if (input.length() != 9) {
                return false;
            }
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void logNumber() {
        int currentTotal = usedNumbers.size();
        int duplicates = duplicateNumberCount.get() - this.previousDuplicates;
        int newNumbers = currentTotal - previousTotal;
        System.out.println( "Received " + newNumbers+ " unique numbers, " + duplicates + " duplicates. Total unique numbers: " + currentTotal);

        this.previousTotal = currentTotal;
        this.previousDuplicates = duplicateNumberCount.get();
    }
}
