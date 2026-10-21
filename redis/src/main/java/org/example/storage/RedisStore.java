package org.example.storage;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisStore {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(9000));

        // Virtual thread executor - can handle millions of concurrent connections
        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            System.out.println("Virtual thread TCP server on port 9000");

            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(() -> {
                    try (socket;
                         var in = new BufferedReader(
                                 new InputStreamReader(socket.getInputStream()));
                         var out = new PrintWriter(socket.getOutputStream(), true)) {
                        String line;
                        while ((line = in.readLine()) != null) {
                            out.println(line);
                        }
                    } catch (IOException e) { /* handle */ }
                });
            }
        }
    }
}
