package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 6969;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[i + 1]);
                }
            }
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            while(true) {
                Socket client = serverSocket.accept();
                executor.submit(new clientHandler(client));
            }
        }
        finally{
            executor.shutdown();
        }
    }
}