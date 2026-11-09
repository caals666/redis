package org.example;

import org.example.storage.RedisStore;
import org.example.command.CommandHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static int port=6969;
    private static final ExecutorService executor=Executors.newVirtualThreadPerTaskExecutor();
    private static final RedisStore store=new RedisStore();
    private static final CommandHandler commandHandler=new CommandHandler(store);

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--port".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                    port = Integer.parseInt(args[i + 1]);
                    i++;
            }
        }

        try (ServerSocket serverSocket = new ServerSocket(port)){
            while (!serverSocket.isClosed()) {
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