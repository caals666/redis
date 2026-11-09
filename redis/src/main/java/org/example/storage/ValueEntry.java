package org.example.storage;

public class ValueEntry {
    private String value;
    private long expiryTimestampMillis;


    public ValueEntry(String value){
        this(value,-1);
    }

    public ValueEntry(String value, long expiryTimestampMillis){
        this.value = value;
        this.expiryTimestampMillis = expiryTimestampMillis;
    }

    public String getValue() {
        return value;
    }

    public long getExpiryTimestampMillis() {
        return expiryTimestampMillis;
    }

    public boolean hasExpiry(){
        return expiryTimestampMillis>0;
    }

    public boolean isExpired(){
        return this.hasExpiry()&System.currentTimeMillis()>expiryTimestampMillis;
    }

    public long getTtlMillis(){
        if(expiryTimestampMillis<=0){
            return -1;
        }

        long remaining = expiryTimestampMillis-System.currentTimeMillis();
        return Math.max(0,remaining);
    }
}
