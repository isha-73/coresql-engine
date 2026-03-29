package com.coresql.ast;

import java.util.ArrayList;
import java.util.List;

public class CreateTableQuery extends Query {
    public String tableName;
    public List<ColumnDefinition> columns = new ArrayList<>();

    public CreateTableQuery() {
        super(QueryType.CREATE_TABLE);
    }
}
