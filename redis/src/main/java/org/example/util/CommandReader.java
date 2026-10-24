package org.example.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CommandReader {
    private final InputStream in;
    private final OutputStream out;

    public CommandReader(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public void read() throws IOException {
        int first = in.read();
        if (first == -1) return;

        List<String> args = new ArrayList<>();
        if (first == '*') {
            args = readArray();
        }

        if (args == null || args.isEmpty()) {
            return;
        }

        String cmd = args.get(0).toUpperCase();
        switch (cmd) {
            case "PING"    -> writeSimpleString("PONG");
            case "ECHO"    -> {
                if (args.size() != 2) writeError("ERR wrong number of arguments for 'echo' command");
                else                  writeBulk(args.get(1));
            }
            case "COMMAND" -> out.write("*0\r\n".getBytes(StandardCharsets.US_ASCII));
            default        -> writeError("ERR unknown command '" + args.get(0) + "'");
        }
        out.flush();
    }

    private List<String> readArray() throws IOException {
        int count = Integer.parseInt(readLine());
        if (count == -1) return null;
        List<String> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int t = in.read();
            if (t != '$') throw new IOException("expected bulk string, got " + (char) t);
            int len = Integer.parseInt(readLine());
            if (len == -1) {
                items.add(null);
                continue;
            }
            byte[] buf = in.readNBytes(len);
            int cr = in.read();
            int lf = in.read();
            if (cr != '\r' || lf != '\n') throw new IOException("expected CRLF after payload");
            items.add(new String(buf, StandardCharsets.UTF_8));
        }
        return items;
    }

    private String readLine() throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(64);
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                byte[] bytes = buf.toByteArray();
                int len = bytes.length;
                if (len > 0 && bytes[len - 1] == '\r') {
                    len--;
                }
                return new String(bytes, 0, len, StandardCharsets.UTF_8);
            }
            buf.write(b);
        }
        throw new EOFException("unexpected EOF in line");
    }

    private void writeSimpleString(String s) throws IOException {
        out.write(('+' + s + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeError(String s) throws IOException {
        out.write(('-' + s + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private void writeBulk(String s) throws IOException {
        if (s == null) { out.write("$-1\r\n".getBytes(StandardCharsets.US_ASCII)); return; }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.write(('$' + Integer.toString(bytes.length) + "\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
    }
}