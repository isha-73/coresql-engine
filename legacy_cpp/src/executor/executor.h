#pragma once

#include "../ast/query_ast.h"
#include "../storage/storage_engine.h"
#include <memory>
#include <vector>
#include <string>

class Executor {
public:
    Executor(StorageEngine& storage);

    void execute(const std::shared_ptr<Query>& query);

private:
    StorageEngine& storage;

    void execute_create_table(const CreateTableQuery* query);
    void execute_insert(const InsertQuery* query);
    void execute_select(const SelectQuery* query);

    bool evaluate_condition(const std::string& row_val, const std::string& op, const std::string& cond_val);
};
