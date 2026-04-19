package com.coresql.engine;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.coresql.ast.ColumnDefinition;

public class WalManager {
    private final String walDirName = "data/";
    private final String walFilePath = "data/wal.log";
    private final String checkpointFilePath = "data/wal.log.checkpoint";
    
    private long currentLsn = 0;
    private FileOutputStream fos;
    private BufferedWriter writer;

    public WalManager() {
        File dir = new File(walDirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        try {
            File walFile = new File(walFilePath);
            // Append mode
            this.fos = new FileOutputStream(walFile, true);
            this.writer = new BufferedWriter(new OutputStreamWriter(fos));
            
            // Read highest LSN from WAL log to initialize currentLsn
            if (walFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(walFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        WalEntry entry = WalEntry.deserialize(line);
                        if (entry != null && entry.lsn > currentLsn) {
                            currentLsn = entry.lsn;
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize WalManager: " + e.getMessage());
        }
    }

    public synchronized long append(String tableName, String operation, String data) {
        currentLsn++;
        long timestamp = System.currentTimeMillis() / 1000L;
        WalEntry entry = new WalEntry(currentLsn, timestamp, tableName, operation, data);
        try {
            writer.write(entry.serialize());
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write to WAL: " + e.getMessage());
        }
        return currentLsn;
    }

    public synchronized void flush() {
        try {
            if (writer != null) {
                writer.flush();
            }
            if (fos != null) {
                fos.getFD().sync(); // Force flush OS buffers to disk
            }
        } catch (IOException e) {
            System.err.println("Failed to fsync WAL: " + e.getMessage());
        }
    }

    public void recover(StorageEngine storage) {
        long checkpointLsn = 0;
        File checkpointFile = new File(checkpointFilePath);
        if (checkpointFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(checkpointFile))) {
                String line = br.readLine();
                if (line != null && !line.isEmpty()) {
                    checkpointLsn = Long.parseLong(line);
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Failed to read checkpoint: " + e.getMessage());
            }
        }

        File walFile = new File(walFilePath);
        if (!walFile.exists()) return;

        int recoverCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(walFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                WalEntry entry = WalEntry.deserialize(line);
                if (entry != null && entry.lsn > checkpointLsn) {
                    processRecovery(entry, storage);
                    recoverCount++;
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read WAL for recovery: " + e.getMessage());
        }

        if (recoverCount > 0) {
            System.out.println("Recovered " + recoverCount + " operations from WAL.");
            updateCheckpoint(currentLsn);
        }
    }

    private void processRecovery(WalEntry entry, StorageEngine storage) {
        long tableLsn = storage.getTableLsn(entry.tableName);
        if (entry.lsn <= tableLsn) {
            return; // Skip already applied operations to avoid data duplication
        }

        if ("CREATE_TABLE".equals(entry.operation)) {
            File tableFile = new File("tables/" + entry.tableName + ".csv");
            if (!tableFile.exists()) {
                // Determine schema from data
                String[] rawCols = entry.data.split(",");
                List<ColumnDefinition> columns = new ArrayList<>();
                for (String col : rawCols) {
                    // Remove quotes if present
                    col = col.replace("\"", "");
                    String[] parts = col.split(":");
                    if (parts.length >= 2) {
                        columns.add(new ColumnDefinition(parts[0], parts[1]));
                    } else {
                        columns.add(new ColumnDefinition(parts[0], "STRING"));
                    }
                }
                storage.createTable(entry.tableName, columns, true);
                storage.updateTableLsn(entry.tableName, entry.lsn);
            }
        } else if ("INSERT".equals(entry.operation)) {
            File tableFile = new File("tables/" + entry.tableName + ".csv");
            if (tableFile.exists()) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(tableFile, true))) {
                    bw.write(entry.data);
                    bw.newLine();
                    storage.updateTableLsn(entry.tableName, entry.lsn);
                } catch (IOException e) {
                    System.err.println("Failed to insert row during recovery: " + e.getMessage());
                }
            }
        }
    }

    public synchronized void updateCheckpoint(long lsn) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(checkpointFilePath))) {
            bw.write(String.valueOf(lsn));
        } catch (IOException e) {
            System.err.println("Failed to write WAL checkpoint: " + e.getMessage());
        }
    }
}
