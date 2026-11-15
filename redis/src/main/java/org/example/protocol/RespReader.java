package org.example.protocol;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RespReader {
    private final InputStream in;
    private final byte[] buffer=new byte[64*1024];
    private int pos=0;
    private int limit=0;

    public RespReader(InputStream in) {
        this.in = in;
    }

    private void fill() throws IOException {
        int n=in.read(buffer, 0, buffer.length);
        pos = 0;
        limit = n;
    }

    private int readByte() throws IOException {
        if(pos==limit){
            fill();
        }
        return Byte.toUnsignedInt(buffer[pos++]);
    }

    public RespValue read() throws IOException {
        int first = readByte();
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
        StringBuilder sb = new StringBuilder();
        byte b;
        while ((b = (byte) readByte()) != -1) {
            if(b=='\n'){
                if(sb.charAt(sb.length()-1)=='\r'){
                    sb.setLength(sb.length()-1);
                }
                return sb.toString();
            }
            sb.append((char)b);
        }
        return sb.toString();
    }

    public RespValue readBulkString() throws IOException {
        int length = Integer.parseInt(readLine());
        if (length == -1) return null;
        if(length==0){
            readByte();
            readByte();
            return RespValue.simpleString("");
        }
        byte[] bytes = new byte[length];
        for(int i=0;i<length;i++){
            int b = readByte();
            if(b==-1) throw new EOFException("Unexpected EOF while reading bulk string");
            bytes[i] = (byte) b;
        }
        int cr = readByte();
        int lf = readByte();

        return RespValue.bulkString(new String(bytes, StandardCharsets.UTF_8));
    }

    public RespValue readInlineCommand(int firstByte) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append((char)firstByte);

        int b;
        while((b=readByte())!=-1){
            if(b=='\r'||b=='\n'){
                break;
            }
            sb.append((char)b);
        }

        String line = sb.toString().trim();
        if (line.isEmpty()) return RespValue.array(new ArrayList<>());

        String[] parts = line.split(" ");
        List<RespValue> items = new ArrayList<>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            items.add(RespValue.bulkString(parts[i]));
        }
        return RespValue.array(items);
    }
}