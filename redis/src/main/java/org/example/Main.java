package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static int port=6969;
    private static ExecutorService executor=Executors.newCachedThreadPool();

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--port".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                    port = Integer.parseInt(args[i + 1]);
                    i++;
            }
        }

        try (ServerSocket serverSocket = new ServerSocket(port)){
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    executor.submit(new clientHandler(client));
                } catch (IOException e) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            executor.shutdown();
        }
    }
}