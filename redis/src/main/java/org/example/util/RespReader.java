package org.example.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RespReader {
    private final InputStream in;
    private final OutputStream out;

    public RespReader(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public RespValue read() throws IOException {
        int first = in.read();
        if (first == -1) return null;

        char prefix = (char) first;
        switch (prefix) {
            case '+'    -> {
                return RespValue.simpleString(readLine());
            }
            case '*'    -> {
                return readArray();
            }
            case '$' -> {
                return readBulkString();
            }
            default -> {
                return readInlineCommand(first);
            }
        }
    }

    private RespValue readArray() throws IOException {
        int count = Integer.parseInt(readLine());
        if (count == -1) return null;
        List<RespValue> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            RespValue item = read();
            if (item == null) {
                throw new EOFException("Unexpected EOF while reading array element " + i + " of " + count);
            }
            items.add(item);
        }
        return RespValue.array(items);
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
        return buf.toString(StandardCharsets.UTF_8);
    }

    public RespValue readBulkString() throws IOException {
        int length = Integer.parseInt(readLine());
        if (length == -1) return null;
        if(length==0){
            readLine();
            return RespValue.simpleString("");
        }
        byte[] bytes = in.readNBytes(length);
        int cr = in.read();
        int lf = in.read();
        if(cr!='\r'||lf!='\n'){
            if(lf=='\n'){
            }
        }

        return RespValue.bulkString(new String(bytes, StandardCharsets.UTF_8));
    }

    public RespValue readInlineCommand(int firstByte) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write(firstByte);

        int b;
        while((b=in.read())!=-1){
            if(b=='\r'||b=='\n'){
                break;
            }
            buf.write(b);
        }

        String line = buf.toString(StandardCharsets.UTF_8).trim();

        String[] parts = line.split(" ");
        List<RespValue> items = new ArrayList<>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            items.add(RespValue.bulkString(parts[i]));
        }
        return RespValue.array(items);
    }
}