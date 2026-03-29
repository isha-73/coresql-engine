package com.coresql.engine;

import com.coresql.ast.*;

import java.util.ArrayList;
import java.util.List;

public class Executor {
    private final StorageEngine storage;

    public Executor(StorageEngine storage) {
        this.storage = storage;
    }

    public void execute(Query query) {
        if (query == null) return;

        switch (query.getType()) {
            case CREATE_TABLE -> executeCreateTable((CreateTableQuery) query);
            case INSERT -> executeInsert((InsertQuery) query);
            case SELECT -> executeSelect((SelectQuery) query);
            default -> System.err.println("Unknown query type.");
        }
    }

    private void executeCreateTable(CreateTableQuery query) {
        if (storage.createTable(query.tableName, query.columns)) {
            System.out.println("Table '" + query.tableName + "' created successfully.");
        }
    }

    private void executeInsert(InsertQuery query) {
        List<ColumnDefinition> schema = storage.readSchema(query.tableName);
        if (schema == null) {
            System.err.println("Table '" + query.tableName + "' does not exist.");
            return;
        }

        if (schema.size() != query.values.size()) {
            System.err.println("Error: Column count mismatch. Expected " + schema.size() + ", got " + query.values.size());
            return;
        }

        // Validate types
        for (int i = 0; i < schema.size(); i++) {
            String expectedType = schema.get(i).type;
            String val = query.values.get(i);
            
            if (expectedType.equals("INT") || expectedType.equals("INTEGER")) {
                try {
                    Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    System.err.println("Error: Value '" + val + "' is not a valid " + expectedType + " for column '" + schema.get(i).name + "'.");
                    return;
                }
            }
        }

        if (storage.insertRow(query.tableName, query.values)) {
            System.out.println("1 row inserted into '" + query.tableName + "'.");
        }
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

    private void executeSelect(SelectQuery query) {
        StorageEngine.TableData data = storage.readTable(query.tableName);
        if (data == null) {
            System.err.println("Failed to read table '" + query.tableName + "'.");
            return;
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
                    System.err.println("Column '" + colName + "' not found.");
                    return;
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
                System.err.println("WHERE column '" + query.whereClause.column + "' not found.");
                return;
            }
            if (whereExpectedType.equals("INT") || whereExpectedType.equals("INTEGER")) {
                try {
                    Integer.parseInt(query.whereClause.value);
                } catch (NumberFormatException e) {
                    System.err.println("Error: WHERE clause value '" + query.whereClause.value + "' is not a valid " + whereExpectedType + ".");
                    return;
                }
            }
        }

        // Print header
        for (int i = 0; i < colIndices.size(); i++) {
            System.out.print(data.columns.get(colIndices.get(i)).name);
            if (i < colIndices.size() - 1) System.out.print(" | ");
        }
        System.out.println();
        System.out.println("-".repeat(Math.max(10, colIndices.size() * 10)));

        // Print rows
        int matchCount = 0;
        for (List<String> row : data.rows) {
            if (query.hasWhere && whereColIdx != -1) {
                if (!evaluateCondition(row.get(whereColIdx), query.whereClause.op, query.whereClause.value, whereExpectedType)) {
                    continue;
                }
            }
            
            for (int i = 0; i < colIndices.size(); i++) {
                System.out.print(row.get(colIndices.get(i)));
                if (i < colIndices.size() - 1) System.out.print(" | ");
            }
            System.out.println();
            matchCount++;
        }
        System.out.println("(" + matchCount + " rows)");
    }
}
