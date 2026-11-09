package org.example.storage;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;

public class RedisStore implements AutoCloseable{
    private final ConcurrentHashMap<String, ValueEntry> store = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;

    public RedisStore(){
        this(true);
    }

    public RedisStore(boolean enableBackgroundCleanup){
        if(enableBackgroundCleanup){
            cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r->{
                Thread t = new Thread(r, "redis-active-expiration");
                t.setDaemon(true);
                return t;
            });
            this.cleanupExecutor.scheduleAtFixedRate(this::cleanupExpired,100,100, TimeUnit.MILLISECONDS);
        }
        else{
            this.cleanupExecutor = null;
        }
    }

    public void set(String key, String value, long expiryMillis){
        if(expiryMillis <= 0){
            store.remove(key);
            return;
        }
        long expiryTimeStamp = System.currentTimeMillis() + expiryMillis;
        store.put(key, new ValueEntry(value, expiryTimeStamp));
    }

    public String get(){
        return "";
    }

    public void cleanupExpired(){
        long now = System.currentTimeMillis();
        for(Map.Entry<String, ValueEntry> entry : store.entrySet()){
            ValueEntry v = entry.getValue();
            if(v.hasExpiry()&&v.getExpiryTimestampMillis() <= now){
                store.remove(entry.getKey(), v);
            }
        }
    }

    @Override
    public void close() throws Exception {
        if(cleanupExecutor != null){
            cleanupExecutor.shutdown();
            try{
                if(!cleanupExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)){
                    cleanupExecutor.shutdownNow();
                }
            } catch(InterruptedException e){
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
