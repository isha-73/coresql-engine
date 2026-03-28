package com.coresql.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StorageEngine {
    private final String tablesDirectory = "tables/";

    public StorageEngine() {
        File dir = new File(tablesDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String getTablePath(String tableName) {
        return tablesDirectory + tableName + ".csv";
    }

    public boolean createTable(String tableName, List<String> columns) {
        String path = getTablePath(tableName);
        File file = new File(path);
        
        if (file.exists()) {
            System.err.println("Table '" + tableName + "' already exists.");
            return false;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(String.join(",", columns));
            bw.newLine();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to create table file: " + path);
            return false;
        }
    }

    public boolean insertRow(String tableName, List<String> values) {
        String path = getTablePath(tableName);
        File file = new File(path);
        
        if (!file.exists()) {
            System.err.println("Table '" + tableName + "' does not exist.");
            return false;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            bw.write(String.join(",", values));
            bw.newLine();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to insert row: " + path);
            return false;
        }
    }

    public static class TableData {
        public List<String> columns = new ArrayList<>();
        public List<List<String>> rows = new ArrayList<>();
    }

    public TableData readTable(String tableName) {
        String path = getTablePath(tableName);
        File file = new File(path);
        
        if (!file.exists()) {
            System.err.println("Table '" + tableName + "' does not exist.");
            return null;
        }

        TableData data = new TableData();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line == null) {
                return null; // Empty file
            }
            // Read header
            data.columns.addAll(Arrays.asList(line.split(",", -1)));

            // Read rows
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                data.rows.add(Arrays.asList(line.split(",", -1)));
            }
            return data;
        } catch (IOException e) {
            System.err.println("Failed to read table: " + path);
            return null;
        }
    }
}
