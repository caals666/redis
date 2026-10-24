package org.example;

import org.example.util.*;

import java.io.*;
import java.net.*;

public class clientHandler implements Runnable {
    private final Socket clientSocket;

    public clientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (Socket client = clientSocket;
             BufferedInputStream in  = new BufferedInputStream(client.getInputStream());
             BufferedOutputStream out = new BufferedOutputStream(client.getOutputStream())) {

            System.out.println("Client connected: " + client.getRemoteSocketAddress());

            CommandReader reader = new CommandReader(in, out);
            while (clientSocket.isConnected()) {
                reader.read();
            }

        } catch (IOException e) {
            System.out.println("Client Disconnected from "
                    + clientSocket.getRemoteSocketAddress()+"\n");
        }
        finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
