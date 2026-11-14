package org.example;

import org.example.command.Command;
import org.example.command.CommandHandler;
import org.example.protocol.RespReader;
import org.example.protocol.RespValue;
import org.example.protocol.RespWriter;
import org.example.storage.RedisStore;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class clientHandler implements Runnable {
    private final Socket clientSocket;
    private final CommandHandler commandHandler;

    public clientHandler(Socket clientSocket, CommandHandler commandHandler) {
        this.clientSocket = clientSocket;
        this.commandHandler = commandHandler;
    }

    @Override
    public void run() {
        try (Socket client = clientSocket;
             BufferedInputStream in  = new BufferedInputStream(client.getInputStream());
             BufferedOutputStream out = new BufferedOutputStream(client.getOutputStream());) {

            System.out.println("Client connected: " + client.getRemoteSocketAddress());

            RespReader reader = new RespReader(in, out);
            RespValue value = null;
            while (clientSocket.isConnected()) {
                try {
                    value = reader.read();
                    System.out.println(value);
                    Command command = Command.fromRespValue(value);
                    System.out.println("command in clientHandler: "+command);
                    RespValue response = commandHandler.execute(command);
                    System.out.println("response in clientHandler: "+response);

                    RespWriter.write(out, response);
                    out.flush();
                } catch (Exception e) {
                    System.out.println("Breaks in clientHandler");
                    break;
                }
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
