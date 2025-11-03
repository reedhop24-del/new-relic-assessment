package com.newRelic.assessment.server;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import static org.junit.Assert.*;

public class ShutdownTest {

    @Test
    public void clientSendsTerminate_closesAllConnections() throws IOException {
        Socket socket = new Socket("127.0.0.1", 4000);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("terminate");
        String response = in.readLine();

        System.out.println("Response from server: " +  response);
        assertNull(response);
    }
}
