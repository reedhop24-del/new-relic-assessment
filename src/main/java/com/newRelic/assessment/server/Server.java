package com.newRelic.assessment.server;

public class Server {

    public static void main(String[] args) {
        System.out.println("Started NR com.newRelic.example.socket.Server");
        SocketHandler socket = new SocketHandler();
        try {
            int PORT = 4000;
            socket.start(PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
