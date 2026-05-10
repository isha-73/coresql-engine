package com.coresql.engine;

import com.coresql.ast.ColumnDefinition;

import java.util.ArrayList;
import java.util.List;

public class TableData {
    public List<ColumnDefinition> columns = new ArrayList<>();
    public List<List<String>> rows = new ArrayList<>();
}
