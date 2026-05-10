package com.coresql.engine;

import java.util.Base64;

public class WalEntry {
    public final long lsn;
    public final long timestamp;
    public final String tableName;
    public final String operation;
    public final byte[] data;

    public WalEntry(long lsn, long timestamp, String tableName, String operation, byte[] data) {
        this.lsn = lsn;
        this.timestamp = timestamp;
        this.tableName = tableName;
        this.operation = operation;
        this.data = data;
    }

    public String serialize() {
        String base64Data = Base64.getEncoder().encodeToString(data);
        return lsn + "|" + timestamp + "|" + tableName + "|" + operation + "|" + base64Data;
    }

    public static WalEntry deserialize(String line) {
        String[] parts = line.split("\\|", 5);
        if (parts.length != 5) return null;
        try {
            long lsn = Long.parseLong(parts[0]);
            long timestamp = Long.parseLong(parts[1]);
            byte[] data = Base64.getDecoder().decode(parts[4]);
            return new WalEntry(lsn, timestamp, parts[2], parts[3], data);
        } catch (Exception e) {
            return null;
        }
    }
}
