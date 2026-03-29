package com.coresql.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.coresql.ast.ColumnDefinition;

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

    private String encodeCsvRow(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (value == null) value = "";
            boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
            if (needsQuotes) {
                sb.append("\"").append(value.replace("\"", "\"\"")).append("\"");
            } else {
                sb.append(value);
            }
            if (i < values.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private List<String> readCsvRow(BufferedReader br) throws IOException {
        String line = br.readLine();
        if (line == null) return null;

        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        while (true) {
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (inQuotes) {
                    if (c == '"') {
                        if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                            current.append('"');
                            i++;
                        } else {
                            inQuotes = false;
                        }
                    } else {
                        current.append(c);
                    }
                } else {
                    if (c == '"') {
                        inQuotes = true;
                    } else if (c == ',') {
                        values.add(current.toString());
                        current.setLength(0);
                    } else {
                        current.append(c);
                    }
                }
            }
            if (inQuotes) {
                current.append("\n");
                line = br.readLine();
                if (line == null) break;
            } else {
                break;
            }
        }
        values.add(current.toString());
        return values;
    }

    public List<ColumnDefinition> readSchema(String tableName) {
        String path = getTablePath(tableName);
        File file = new File(path);
        
        if (!file.exists()) return null;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            List<String> header = readCsvRow(br);
            if (header == null) return null;

            List<ColumnDefinition> schema = new ArrayList<>();
            for (String h : header) {
                String[] parts = h.split(":");
                if (parts.length >= 2) {
                    schema.add(new ColumnDefinition(parts[0], parts[1]));
                } else {
                    schema.add(new ColumnDefinition(h, "STRING"));
                }
            }
            return schema;
        } catch (IOException e) {
            return null;
        }
    }

    public boolean createTable(String tableName, List<ColumnDefinition> columns) {
        String path = getTablePath(tableName);
        File file = new File(path);
        
        if (file.exists()) {
            System.err.println("Table '" + tableName + "' already exists.");
            return false;
        }

        List<String> encodedCols = new ArrayList<>();
        for (ColumnDefinition cd : columns) {
            encodedCols.add(cd.name + ":" + cd.type);
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(encodeCsvRow(encodedCols));
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

        // Validate arity against table header
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            List<String> header = readCsvRow(br);
            if (header != null) {
                int expectedCols = header.size();
                if (values.size() != expectedCols) {
                    System.err.println("Error: Column count mismatch. Expected " + expectedCols + ", got " + values.size());
                    return false;
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read header for validation: " + path);
            return false;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            bw.write(encodeCsvRow(values));
            bw.newLine();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to insert row: " + path);
            return false;
        }
    }

    public static class TableData {
        public List<ColumnDefinition> columns = new ArrayList<>();
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
            List<String> header = readCsvRow(br);
            if (header == null) {
                return null; // Empty file
            }
            // Read header
            for (String h : header) {
                String[] parts = h.split(":");
                if (parts.length >= 2) {
                    data.columns.add(new ColumnDefinition(parts[0], parts[1]));
                } else {
                    data.columns.add(new ColumnDefinition(h, "STRING"));
                }
            }

            // Read rows
            List<String> row;
            while ((row = readCsvRow(br)) != null) {
                if (row.size() == 1 && row.get(0).isEmpty()) continue;
                data.rows.add(row);
            }
            return data;
        } catch (IOException e) {
            System.err.println("Failed to read table: " + path);
            return null;
        }
    }
}
