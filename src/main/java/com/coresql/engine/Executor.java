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
        if (storage.insertRow(query.tableName, query.values)) {
            System.out.println("1 row inserted into '" + query.tableName + "'.");
        }
    }

    private boolean evaluateCondition(String rowVal, String op, String condVal) {
        try {
            int rVal = Integer.parseInt(rowVal);
            int cVal = Integer.parseInt(condVal);
            switch (op) {
                case "=" -> { return rVal == cVal; }
                case "<" -> { return rVal < cVal; }
                case ">" -> { return rVal > cVal; }
            }
        } catch (NumberFormatException e) {
            // Fallback to string comparison
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
                int index = data.columns.indexOf(colName);
                if (index != -1) {
                    colIndices.add(index);
                } else {
                    System.err.println("Column '" + colName + "' not found.");
                    return;
                }
            }
        }

        int whereColIdx = -1;
        if (query.hasWhere) {
            whereColIdx = data.columns.indexOf(query.whereClause.column);
            if (whereColIdx == -1) {
                System.err.println("WHERE column '" + query.whereClause.column + "' not found.");
                return;
            }
        }

        // Print header
        for (int i = 0; i < colIndices.size(); i++) {
            System.out.print(data.columns.get(colIndices.get(i)));
            if (i < colIndices.size() - 1) System.out.print(" | ");
        }
        System.out.println();
        System.out.println("-".repeat(Math.max(10, colIndices.size() * 10)));

        // Print rows
        int matchCount = 0;
        for (List<String> row : data.rows) {
            if (query.hasWhere && whereColIdx != -1) {
                if (!evaluateCondition(row.get(whereColIdx), query.whereClause.op, query.whereClause.value)) {
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
