//package org.example.util;
//
//import java.io.BufferedOutputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.nio.charset.StandardCharsets;
//
//public class CommandWriter {
//    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
//    private static final byte[] NULL_BULK_STRING_BYTES = "$-1\r\n".getBytes(StandardCharsets.UTF_8);
//    private static final byte[] OK_BYTES = "+OK\r\n".getBytes(StandardCharsets.UTF_8);
//    private static final byte[] PONG_BYTES = "+PONG\r\n".getBytes(StandardCharsets.UTF_8);
//    public static void write(BufferedOutputStream out, CommandValue value) throws IOException {
//        if (value == null) {
//            writeNullBulkString(out);
//            return;
//        }
//        out.write(value.encode());
//    }
//
//    public static void writeNullBulkString(OutputStream out) throws IOException {
//        out.write(NULL_BULK_STRING_BYTES);
//    }
//}
