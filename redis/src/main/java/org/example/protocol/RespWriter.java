package org.example.protocol;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class RespWriter {
    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NULL_BULK_STRING_BYTES = "$-1\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OK_BYTES = "+OK\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PONG_BYTES = "+PONG\r\n".getBytes(StandardCharsets.UTF_8);
    public static void write(OutputStream out, RespValue value) throws IOException {
        if (value == null) {
            writeNullBulkString(out);
            return;
        }
        System.out.println(Arrays.toString(value.encode()));
        out.write(value.encode());
    }

    public static void writeSimpleString(OutputStream out, String value) throws IOException {
        if("OK".equals(value)){
            out.write(OK_BYTES);
        }
        else if("PING".equals(value)){
            out.write(PONG_BYTES);
        }
        else{
            out.write(('+'+value+"\r\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void writeError(OutputStream out, RespValue value) throws IOException {
        out.write(("-"+value+"\r\n").getBytes(StandardCharsets.UTF_8));
    }

    public static void writeInteger(OutputStream out, RespValue value) throws IOException {
        out.write((":"+String.valueOf(value)+"\r\n").getBytes(StandardCharsets.UTF_8));
    }

    public static void writeBulkString(OutputStream out, String str) throws IOException {
        if (str == null) {
            writeNullBulkString(out);
            return;
        }
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        String header = "$" + bytes.length + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.write(CRLF);
    }

    public static void writeArray(OutputStream out, List<RespValue> elements) throws IOException {
        if (elements == null) {
            out.write("*-1\r\n".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String header = "*" + elements.size() + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        for (RespValue elem : elements) {
            write(out, elem);
        }
    }

    public static void writeNullBulkString(OutputStream out) throws IOException {
        out.write(NULL_BULK_STRING_BYTES);
    }
}
