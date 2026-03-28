package com.coresql.ast;

public abstract class Query {
    private final QueryType type;

    protected Query(QueryType type) {
        this.type = type;
    }

    public QueryType getType() {
        return type;
    }
}
