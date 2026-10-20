package org.example;

import com.beust.jcommander.JCommander;
import org.example.config.Args;

import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] argv) throws IOException {

        Args args = new Args();
        JCommander jc = new JCommander(args);

        try{
            jc.parse(argv);
        }
        catch (Exception ex){
            jc.usage();
        }

        if(args.help){
            jc.usage();
            System.exit(0);
        }

        try{
            ServerSocket serverSocket = new ServerSocket(args.port);
        }
        catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }
}
