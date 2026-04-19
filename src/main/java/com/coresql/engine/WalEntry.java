package com.coresql.engine;

public class WalEntry {
    public final long lsn;
    public final long timestamp;
    public final String tableName;
    public final String operation;
    public final String data;

    public WalEntry(long lsn, long timestamp, String tableName, String operation, String data) {
        this.lsn = lsn;
        this.timestamp = timestamp;
        this.tableName = tableName;
        this.operation = operation;
        this.data = data;
    }

    public String serialize() {
        return lsn + "|" + timestamp + "|" + tableName + "|" + operation + "|" + data;
    }

    public static WalEntry deserialize(String line) {
        String[] parts = line.split("\\|", 5);
        if (parts.length != 5) return null;
        try {
            long lsn = Long.parseLong(parts[0]);
            long timestamp = Long.parseLong(parts[1]);
            return new WalEntry(lsn, timestamp, parts[2], parts[3], parts[4]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
