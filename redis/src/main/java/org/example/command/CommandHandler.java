package org.example.command;

import org.example.protocol.RespValue;
import org.example.storage.RedisStore;

public class CommandHandler {
    private final RedisStore store;

    public CommandHandler(RedisStore store) {
        this.store = store;
    }

    public static RespValue execute(Command command) {
        return RespValue.simpleString("");
    }
}
