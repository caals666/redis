package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

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

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port + ". Waiting for connections...");

            // Loop to handle new connections
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    System.out.println("Client connected!");

                    InputStream inputStream = socket.getInputStream();
                    BufferedReader in_socket = new BufferedReader(new InputStreamReader(inputStream));
                    OutputStream outputStream = socket.getOutputStream();
                    PrintWriter out = new PrintWriter(outputStream, true);
                    String value=in_socket.readLine();

                    while (!socket.isClosed()) {
                        try {
                            value = in_socket.readLine();
                        } catch (EOFException | SocketException e) {
                            // Client disconnected
                            break;
                        } catch (IOException e) {
                            if (socket.isClosed()) {
                                break;
                            }
                            System.err.println("I/O error reading from client: " + e.getMessage());
                            break;
                        }

                        if (value == null) {
                            // Stream closed
                            break;
                        }
                        out.print("+PONG\r\n");
                        out.flush();
                    }

                    System.out.println("Client disconnected.");
                } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        }
    }
}