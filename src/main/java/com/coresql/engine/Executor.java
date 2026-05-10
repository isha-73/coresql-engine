package com.coresql.engine;

import com.coresql.ast.*;

import java.util.ArrayList;
import java.util.List;

public class Executor {
    private final StorageEngine storage;

    public Executor(StorageEngine storage) {
        this.storage = storage;
    }

    public String execute(Query query) {
        if (query == null) return "Error: Query is null";

        switch (query.getType()) {
            case CREATE_TABLE -> { return executeCreateTable((CreateTableQuery) query); }
            case INSERT -> { return executeInsert((InsertQuery) query); }
            case SELECT -> { return executeSelect((SelectQuery) query); }
            default -> { return "Error: Unknown query type."; }
        }
    }

    private String executeCreateTable(CreateTableQuery query) {
        if (storage.createTable(query.tableName, query.columns)) {
            return "Table '" + query.tableName + "' created successfully.";
        }
        return "Error: Failed to create table '" + query.tableName + "'.";
    }

    private String executeInsert(InsertQuery query) {
        List<ColumnDefinition> schema = storage.readSchema(query.tableName);
        if (schema == null) {
            return "Error: Table '" + query.tableName + "' does not exist.";
        }

        if (schema.size() != query.values.size()) {
            return "Error: Column count mismatch. Expected " + schema.size() + ", got " + query.values.size();
        }

        // Validate types
        for (int i = 0; i < schema.size(); i++) {
            String expectedType = schema.get(i).type;
            String val = query.values.get(i);
            
            if (expectedType.equals("INT") || expectedType.equals("INTEGER")) {
                try {
                    Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return "Error: Value '" + val + "' is not a valid " + expectedType + " for column '" + schema.get(i).name + "'.";
                }
            }
        }

        if (storage.insertRow(query.tableName, query.values)) {
            return "1 row inserted into '" + query.tableName + "'.";
        }
        return "Error: Failed to insert row into '" + query.tableName + "'.";
    }

    private boolean evaluateCondition(String rowVal, String op, String condVal, String type) {
        if (type.equals("INT") || type.equals("INTEGER")) {
            try {
                int rVal = Integer.parseInt(rowVal);
                int cVal = Integer.parseInt(condVal);
                switch (op) {
                    case "=" -> { return rVal == cVal; }
                    case "<" -> { return rVal < cVal; }
                    case ">" -> { return rVal > cVal; }
                }
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            switch (op) {
                case "=" -> { return rowVal.equals(condVal); }
                case "<" -> { return rowVal.compareTo(condVal) < 0; }
                case ">" -> { return rowVal.compareTo(condVal) > 0; }
            }
        }
        return false;
    }

    private String executeSelect(SelectQuery query) {
        TableData data = storage.readTable(query.tableName);
        if (data == null) {
            return "Error: Failed to read table '" + query.tableName + "'.";
        }

        List<Integer> colIndices = new ArrayList<>();
        if (query.columns.size() == 1 && query.columns.get(0).equals("*")) {
            for (int i = 0; i < data.columns.size(); i++) {
                colIndices.add(i);
            }
        } else {
            for (String colName : query.columns) {
                int index = -1;
                for (int i = 0; i < data.columns.size(); i++) {
                    if (data.columns.get(i).name.equals(colName)) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    colIndices.add(index);
                } else {
                    return "Error: Column '" + colName + "' not found.";
                }
            }
        }

        int whereColIdx = -1;
        String whereExpectedType = "STRING";
        if (query.hasWhere) {
            for (int i = 0; i < data.columns.size(); i++) {
                if (data.columns.get(i).name.equals(query.whereClause.column)) {
                    whereColIdx = i;
                    whereExpectedType = data.columns.get(i).type;
                    break;
                }
            }
            if (whereColIdx == -1) {
                return "Error: WHERE column '" + query.whereClause.column + "' not found.";
            }
            if (whereExpectedType.equals("INT") || whereExpectedType.equals("INTEGER")) {
                try {
                    Integer.parseInt(query.whereClause.value);
                } catch (NumberFormatException e) {
                    return "Error: WHERE clause value '" + query.whereClause.value + "' is not a valid " + whereExpectedType + ".";
                }
            }
        }

        StringBuilder sb = new StringBuilder();

        // Print header
        for (int i = 0; i < colIndices.size(); i++) {
            sb.append(data.columns.get(colIndices.get(i)).name);
            if (i < colIndices.size() - 1) sb.append(" | ");
        }
        sb.append("\n");
        sb.append("-".repeat(Math.max(10, colIndices.size() * 10))).append("\n");

        // Print rows
        int matchCount = 0;
        for (List<String> row : data.rows) {
            if (query.hasWhere && whereColIdx != -1) {
                if (!evaluateCondition(row.get(whereColIdx), query.whereClause.op, query.whereClause.value, whereExpectedType)) {
                    continue;
                }
            }
            
            for (int i = 0; i < colIndices.size(); i++) {
                sb.append(row.get(colIndices.get(i)));
                if (i < colIndices.size() - 1) sb.append(" | ");
            }
            sb.append("\n");
            matchCount++;
        }
        sb.append("(").append(matchCount).append(" rows)");
        return sb.toString();
    }
}
