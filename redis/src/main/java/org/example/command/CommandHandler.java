package org.example.command;

import org.example.protocol.RespReader;
import org.example.protocol.RespValue;
import org.example.storage.RedisStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommandHandler {
    private static RedisStore store;

    public CommandHandler(RedisStore store) {
        this.store = store;
    }

    public static RespValue execute(Command command) {
        if(command == null){
            return RespValue.error("Command is null");
        }
        String name = command.getName();
        switch (name) {
            case "PING":
                return handlePing(command);
            case "ECHO":
                return handleEcho(command);
            case "SET":
                return handleSet(command);
            case "GET":
                return handleGet(command);
            case "DEL":
                return handleDel(command);
            case "EXISTS":
                return handleExists(command);
            case "COMMAND":
                return handleCommand(command);
            case "INFO":
                return handleInfo(command);
            default:
                return RespValue.error("ERR unknown command '" + name.toLowerCase(Locale.ROOT) + "'");
        }
    }

    private static RespValue handleInfo(Command command) {
        String info = "# Server\r\nredis_version:8.10.2\r\nrole:master\r\n";
        return RespValue.bulkString(info);
    }

    private static RespValue handleCommand(Command command) {
        return RespValue.array(new ArrayList<>());
    }

    private static RespValue handleExists(Command command) {
        if (command.getArgCount() != 1) {
            return RespValue.error("ERR wrong number of arguments for 'exists' command");
        }
        boolean exists = store.exists(command.getArg(0));
        return RespValue.integer(exists ? 1 : 0);
    }

    private static RespValue handleDel(Command command) {
        if(command.getArgCount()<1){
            return RespValue.error("ERR no arguments");
        }
        String[] keys = command.getArgs().toArray(new String[0]);
        int count = store.del(keys);
        return RespValue.integer(count);
    }

    private static RespValue handleSet(Command command) {
        if(command.getArgCount()<2){
            return RespValue.error("ERR command has to be at least 2 arguments");
        }

        String key = command.getArg(0);
        String value = command.getArg(1);
        Long expiryMillis = null;
        for(int i=2;i<command.getArgCount();i++){
            String option = command.getArg(i).toUpperCase(Locale.ROOT);
            if(option.equals("EX")) {
                try {
                    long ex = Long.parseLong(command.getArg(i + 1));
                    if (ex <= 0) {
                        return RespValue.error("ERR invalid expire time in 'set' command");
                    }
                    ex = ex * 1000L;
                    i++;
                } catch (NumberFormatException e) {
                    return RespValue.error("ERR invalid expire time in 'set' command");
                }
            }
            else if ("PX".equals(option)) {
                if (i + 1 >= command.getArgCount()) {
                    return RespValue.error("ERR syntax error");
                }
                try {
                    long px = Long.parseLong(command.getArg(i + 1));
                    if (px <= 0) {
                        return RespValue.error("ERR invalid expire time in 'set' command");
                    }
                    expiryMillis = px;
                    i++;
                } catch (NumberFormatException e) {
                    return RespValue.error("ERR value is not an integer or out of range");
                }
            }
            else {
                return RespValue.error("ERR syntax error");
            }
        }

        if(expiryMillis != null){
            store.set(key, value, expiryMillis);
        }
        else{
            store.set(key, value);
        }
        return RespValue.simpleString("OK");
    }

    private static RespValue handleGet(Command command) {
        if(command.getArgCount()!=1){
            return RespValue.error("ERR command has to be at least 1 argument");
        }
        String key = command.getArg(0);
        String value = store.get(key);
        if(value == null){
            return RespValue.error("ERR no key found");
        }
        return RespValue.bulkString(value);
    }

    private static RespValue handleEcho(Command command) {
        if (command.getArgCount() != 1) {
            return RespValue.error("ERR wrong number of arguments for 'echo' command");
        }
        return RespValue.bulkString(command.getArg(0));
    }

    private static RespValue handlePing(Command command) {
        if(command.getArgCount()==0){
            return RespValue.simpleString("PONG");
        }
        else if(command.getArgCount()==1){
            return RespValue.bulkString(command.getArg(0));
        }
        else{
            return RespValue.error("ERR wrong number of arguments for 'ping' command");
        }
    }
}
