package com.newRelic.assessment.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SocketTest {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    @Before
    public void setUp() throws IOException {
        socket = new Socket("127.0.0.1", 4000);
        socket.setSoTimeout(2000);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @After
    public void tearDown() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (socket != null && !socket.isClosed()) socket.close();
    }

    @Test(expected = SocketTimeoutException.class)
    public void inputValidNumber_clientTimesOut() throws IOException {
        out.println("900987658");
        in.readLine();
    }

    @Test(expected = SocketTimeoutException.class)
    public void inputValidNumber_leadingZeros_clientTimesOut() throws IOException {
        out.println("000987658");
        in.readLine();
    }

    @Test
    public void inputInvalidNumber_tooLong_connectionCloses() throws IOException {
        out.println("000987658999");
        String response = in.readLine();
        assertNull(response);
    }

    @Test
    public void inputInvalidNumber_nonInt_connectionCloses() throws IOException {
        out.println("00098765L\n");
        String response = in.readLine();
        assertNull(response);
    }

    @Test
    public void inputInvalidNumber_doesNotEndWithNewLine_connectionCloses() throws IOException {
        out.println("00098765L");
        String response = in.readLine();
        assertNull(response);
    }

    @Test
    public void testValidInput_readNumbersLog_onlyUniqueInputs() throws Exception {
        out.println("123456789");
        out.println("123456789");
        out.println("234567891");

        Path logPath = Path.of("numbers.log");
        assertTrue(Files.exists(logPath));
        List<String> lines = Files.readAllLines(logPath);
        assertFalse(lines.isEmpty());

        for (String line : lines) {
            assertEquals(9, line.length());
            Integer.parseInt(line);
        }
    }

    @Test
    public void twoClientsSendDuplicateNumber_onlyOneIsLogged() throws Exception {
        String duplicateNumber = "987654321";

        Runnable clientTask = () -> {
            try (Socket clientSocket = new Socket("127.0.0.1", 4000);
                 PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true)) {
                 clientOut.println(duplicateNumber);
            } catch (IOException e) {
                fail("Client failed: " + e.getMessage());
            }
        };

        Thread client1 = new Thread(clientTask);
        Thread client2 = new Thread(clientTask);

        client1.start();
        client2.start();
        client1.join();
        client2.join();

        Path logPath = Path.of("numbers.log");
        assertTrue(Files.exists(logPath));
        List<String> lines = Files.readAllLines(logPath);

        long count = lines.stream().filter(line -> line.equals(duplicateNumber)).count();
        assertEquals("Duplicate number should appear only once", 1, count);
    }


    @Test
    public void onlyFiveClientsCanConnect_sixthClientIsQueuedOrBlocked() throws Exception {
        List<Socket> sockets = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            Socket s = new Socket("127.0.0.1", 4000);
            s.setSoTimeout(2000);
            sockets.add(s);
        }

        Socket sixthSocket = new Socket();
        try {
            sixthSocket.setSoTimeout(2000);
            sixthSocket.connect(new java.net.InetSocketAddress("127.0.0.1", 4000), 1000);

            PrintWriter out = new PrintWriter(sixthSocket.getOutputStream(), true);
            out.println("234567891");
            BufferedReader in = new BufferedReader(new InputStreamReader(sixthSocket.getInputStream()));

            // ExecutorService has queued the message up, below should timeout
            in.readLine();
            fail("6th client should be queued.");
        } catch (IOException e) {
            sixthSocket.close();
            assertTrue(e.getMessage().contains("timed out") || e.getMessage().contains("refused"));
        }
        for (Socket s : sockets) {
            s.close();
        }
    }

    @Test
    public void onlyFiveClientsCanConnect_sixthClientIsQueued_clientFiveDisconnects_clientSixConnects() throws Exception {
        List<Socket> sockets = new ArrayList<>();

        // @Before is connecting 1 client so we only need 4 more here
        for (int i = 0; i < 4; i++) {
            Socket s = new Socket("127.0.0.1", 4000);
            s.setSoTimeout(2000);
            sockets.add(s);
        }

        Socket sixthSocket = new Socket();
        sixthSocket.setSoTimeout(2000);
        sixthSocket.connect(new java.net.InetSocketAddress("127.0.0.1", 4000), 1000);

        PrintWriter out = new PrintWriter(sixthSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(sixthSocket.getInputStream()));

        // Fifth client disconnects and frees up a thread
        sockets.get(3).close();
        out.println("23456789L");

        // Now the sixth client should be able to send bad data and gets disconnected
        String response = in.readLine();
        assertNull(response);
    }

    @Test
    public void clientSendsNoNewLine_serverIgnores_notSentToOutputFile() throws IOException {
        Path logPath = Path.of("numbers.log");
        List<String> beforeWriteLines = Files.readAllLines(logPath);
        assertFalse(beforeWriteLines.isEmpty());

        out.write(Arrays.toString("222222222".getBytes()));
        out.flush();

        List<String> afterWriteLines = Files.readAllLines(logPath);
        assertEquals(beforeWriteLines.size(), afterWriteLines.size());
    }
}
