package com.coresql.ast;

import java.util.ArrayList;
import java.util.List;

public class InsertQuery extends Query {
    public String tableName;
    public List<String> values = new ArrayList<>();

    public InsertQuery() {
        super(QueryType.INSERT);
    }
}
