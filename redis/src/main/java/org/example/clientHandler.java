package org.example;

import java.io.*;
import java.net.*;
import java.util.Objects;

public class clientHandler implements Runnable {
    private final Socket clientSocket;

    public clientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try(Socket client = clientSocket;
        BufferedReader in_socket = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out_socket = new PrintWriter(client.getOutputStream(), true);){
            System.out.println("Client connected at: "+clientSocket.getInetAddress()+":"+clientSocket.getPort());
            String input;
            while ((input = in_socket.readLine()) != null) {
                if ("COMMAND".equals(input)) {
                    out_socket.print("*0\r\n");
                    out_socket.flush();
                }
                if ("PING".equals(input)) {
                    out_socket.print("+PONG\r\n");
                    out_socket.flush();
                }
            }

        } catch(IOException e){
            System.err.println("Connection error from "
                    + clientSocket.getRemoteSocketAddress() + ": " + e.getMessage());
            }
    }
}
