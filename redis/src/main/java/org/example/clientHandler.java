package org.example;

import org.example.command.Command;
import org.example.command.CommandHandler;
import org.example.protocol.RespReader;
import org.example.protocol.RespValue;
import org.example.util.RespWriter;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

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

            RespReader reader = new RespReader(in, out);
            RespValue value = null;
            while (clientSocket.isConnected()) {
                try {
                    value = reader.read();
                    if (value.getStringValue().equals("COMMAND")) {
                        String emptyArray = "*0\r\n";
                        out.write(emptyArray.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    }
                } catch (Exception e) {
                    break;
                }
            }
            Command command = Command.fromRespValue(value);
            RespValue response = CommandHandler.execute(command);

            RespWriter.write(out, response);
            out.flush();

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
