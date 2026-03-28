#pragma once

#include <string>
#include <vector>

// Type of the AST Query Node
enum class QueryType {
    CREATE_TABLE,
    INSERT,
    SELECT,
    UNKNOWN
};

// Base class for all queries
struct Query {
    QueryType type = QueryType::UNKNOWN;
    virtual ~Query() = default;
};

// CREATE TABLE
struct CreateTableQuery : public Query {
    std::string table_name;
    std::vector<std::string> columns;

    CreateTableQuery() { type = QueryType::CREATE_TABLE; }
};

// INSERT INTO
struct InsertQuery : public Query {
    std::string table_name;
    std::vector<std::string> values;

    InsertQuery() { type = QueryType::INSERT; }
};

// Condition representing a WHERE clause
struct Condition {
    std::string column;
    std::string op; // =, >, <
    std::string value;
};

// SELECT
struct SelectQuery : public Query {
    std::vector<std::string> columns;
    std::string table_name;
    bool has_where = false;
    Condition where_clause;

    SelectQuery() { type = QueryType::SELECT; }
};
