package com.coresql.engine;

import com.coresql.ast.ColumnDefinition;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StorageEngine {
    private final WalManager walManager;
    private final Map<String, PageManager> pageManagers = new HashMap<>();
    private final Map<String, BufferPool> bufferPools = new HashMap<>();
    
    public StorageEngine(WalManager walManager) {
        this.walManager = walManager;
    }
    
    private PageManager getPageManager(String tableName) {
        return pageManagers.computeIfAbsent(tableName, PageManager::new);
    }
    
    private BufferPool getBufferPool(String tableName) {
        return bufferPools.computeIfAbsent(tableName, t -> new BufferPool(getPageManager(t), 64));
    }

    public long getTableLsn(String tableName) {
        String tablesDirectory = "tables/";
        String lsnPath = tablesDirectory + tableName + ".db.lsn";
        File file = new File(lsnPath);
        if (file.exists()) {
            try (java.util.Scanner scanner = new java.util.Scanner(file)) {
                if (scanner.hasNextLong()) {
                    return scanner.nextLong();
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return 0;
    }

    public void updateTableLsn(String tableName, long lsn) {
        String tablesDirectory = "tables/";
        String lsnPath = tablesDirectory + tableName + ".db.lsn";
        try (java.io.PrintWriter pw = new java.io.PrintWriter(lsnPath)) {
            pw.println(lsn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ColumnDefinition> readSchema(String tableName) {
        PageManager pm = getPageManager(tableName);
        if (!pm.exists()) return null;
        try {
            return pm.readSchema();
        } catch (IOException e) {
            return null;
        }
    }

    public boolean createTable(String tableName, List<ColumnDefinition> columns) {
        return createTable(tableName, columns, false);
    }

    public boolean createTable(String tableName, List<ColumnDefinition> columns, boolean isRecovery) {
        PageManager pm = getPageManager(tableName);
        if (!isRecovery && pm.exists()) {
            System.err.println("Table '" + tableName + "' already exists.");
            return false;
        }
        
        long lsn = -1;
        if (!isRecovery && walManager != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                sb.append(columns.get(i).name).append(":").append(columns.get(i).type);
                if (i < columns.size() - 1) sb.append(",");
            }
            byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
            lsn = walManager.append(tableName, "CREATE_TABLE", data);
            walManager.flush();
        }
        
        try {
            pm.initializeFile(columns);
            if (!isRecovery && lsn != -1) {
                updateTableLsn(tableName, lsn);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertRow(String tableName, List<String> values) {
        PageManager pm = getPageManager(tableName);
        if (!pm.exists()) return false;
        
        BufferPool pool = getBufferPool(tableName);
        try {
            List<ColumnDefinition> schema = pm.readSchema();
            byte[] rowData = RowSerializer.serialize(values, schema);
            
            long lsn = -1;
            if (walManager != null) {
                lsn = walManager.append(tableName, "INSERT", rowData);
                walManager.flush();
            }

            int numPages = pm.getNumPages();
            if (numPages == 0) {
                numPages = 1;
            }
            
            Page lastPage = pool.getPage(numPages - 1);
            if (!lastPage.insertRow(rowData)) {
                Page newPage = pool.getPage(numPages);
                newPage.insertRow(rowData);
                pool.flushPage(newPage);
            } else {
                pool.flushPage(lastPage);
            }
            
            if (lsn != -1) {
                updateTableLsn(tableName, lsn);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public TableData readTable(String tableName) {
        PageManager pm = getPageManager(tableName);
        if (!pm.exists()) return null;
        
        BufferPool pool = getBufferPool(tableName);
        try {
            TableData data = new TableData();
            data.columns = pm.readSchema();
            int numPages = pm.getNumPages();
            for (int i = 0; i < numPages; i++) {
                Page page = pool.getPage(i);
                List<byte[]> binaryRows = page.readRows();
                for (byte[] binaryRow : binaryRows) {
                    data.rows.add(RowSerializer.deserialize(binaryRow, data.columns));
                }
            }
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void recoverOperation(WalEntry entry) {
        long tableLsn = getTableLsn(entry.tableName);
        if (entry.lsn <= tableLsn) {
            return; // Skip already applied
        }

        if ("CREATE_TABLE".equals(entry.operation)) {
            String schemaStr = new String(entry.data, StandardCharsets.UTF_8);
            String[] rawCols = schemaStr.split(",");
            List<ColumnDefinition> columns = new ArrayList<>();
            for (String col : rawCols) {
                String[] parts = col.split(":");
                if (parts.length >= 2) {
                    columns.add(new ColumnDefinition(parts[0], parts[1]));
                } else {
                    columns.add(new ColumnDefinition(parts[0], "STRING"));
                }
            }
            createTable(entry.tableName, columns, true);
            updateTableLsn(entry.tableName, entry.lsn);

        } else if ("INSERT".equals(entry.operation)) {
            PageManager pm = getPageManager(entry.tableName);
            if (!pm.exists()) return;

            BufferPool pool = getBufferPool(entry.tableName);
            try {
                int numPages = pm.getNumPages();
                if (numPages == 0) numPages = 1;
                
                Page lastPage = pool.getPage(numPages - 1);
                if (!lastPage.insertRow(entry.data)) {
                    Page newPage = pool.getPage(numPages);
                    newPage.insertRow(entry.data);
                    pool.flushPage(newPage);
                } else {
                    pool.flushPage(lastPage);
                }
                updateTableLsn(entry.tableName, entry.lsn);
            } catch (IOException e) {
                System.err.println("Failed to recover INSERT: " + e.getMessage());
            }
        }
    }
}
