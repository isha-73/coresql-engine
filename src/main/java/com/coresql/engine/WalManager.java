package com.coresql.engine;

import java.io.*;

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
            this.fos = new FileOutputStream(walFile, true);
            this.writer = new BufferedWriter(new OutputStreamWriter(fos));
            
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

    public synchronized long append(String tableName, String operation, byte[] data) {
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
                fos.getFD().sync();
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
                    storage.recoverOperation(entry);
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

    public synchronized void updateCheckpoint(long lsn) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(checkpointFilePath))) {
            bw.write(String.valueOf(lsn));
        } catch (IOException e) {
            System.err.println("Failed to write WAL checkpoint: " + e.getMessage());
        }
    }
}
