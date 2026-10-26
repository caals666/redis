package org.example.util;

import org.example.storage.RedisStore;

import java.util.Locale;

public class CommandHandler {
    private final RedisStore store;

    public CommandHandler(RedisStore store) {
        this.store = store;
    }

    
}
