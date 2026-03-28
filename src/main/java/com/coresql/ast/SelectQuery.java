package com.coresql.ast;

import java.util.ArrayList;
import java.util.List;

public class SelectQuery extends Query {
    public List<String> columns = new ArrayList<>();
    public String tableName;
    public boolean hasWhere = false;
    public Condition whereClause;

    public SelectQuery() {
        super(QueryType.SELECT);
    }
}
