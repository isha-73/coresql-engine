package com.coresql.ast;

public class ColumnDefinition {
    public final String name;
    public final String type;

    public ColumnDefinition(String name, String type) {
        this.name = name;
        this.type = type.toUpperCase(); // Normalize INT, STRING etc.
    }

    @Override
    public String toString() {
        return name + ":" + type;
    }
}
