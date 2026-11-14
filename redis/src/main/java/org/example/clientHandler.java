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
             BufferedInputStream in  = new BufferedInputStream(client.getInputStream(), 64 * 1024);
             BufferedOutputStream out = new BufferedOutputStream(client.getOutputStream(), 64 * 1024);) {
            
            RespReader reader = new RespReader(in, out);
            RespValue value = null;
            while (clientSocket.isConnected()) {
                try {
                    value = reader.read();
                    Command command = Command.fromRespValue(value);
                    RespValue response = commandHandler.execute(command);
                    RespWriter.write(out, response);
                    if (in.available() == 0) {
                        out.flush();
                    }
                } catch (Exception e) {
                    break;
                }
            }

        } catch (IOException e) {

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
