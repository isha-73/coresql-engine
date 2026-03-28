#include "executor.h"
#include <iostream>
#include <algorithm>

Executor::Executor(StorageEngine& storage) : storage(storage) {}

void Executor::execute(const std::shared_ptr<Query>& query) {
    if (!query) return;

    switch (query->type) {
        case QueryType::CREATE_TABLE:
            execute_create_table(static_cast<const CreateTableQuery*>(query.get()));
            break;
        case QueryType::INSERT:
            execute_insert(static_cast<const InsertQuery*>(query.get()));
            break;
        case QueryType::SELECT:
            execute_select(static_cast<const SelectQuery*>(query.get()));
            break;
        default:
            std::cerr << "Unknown query type." << std::endl;
    }
}

void Executor::execute_create_table(const CreateTableQuery* query) {
    if (storage.create_table(query->table_name, query->columns)) {
        std::cout << "Table '" << query->table_name << "' created successfully." << std::endl;
    }
}

void Executor::execute_insert(const InsertQuery* query) {
    if (storage.insert_row(query->table_name, query->values)) {
        std::cout << "1 row inserted into '" << query->table_name << "'." << std::endl;
    }
}

bool Executor::evaluate_condition(const std::string& row_val, const std::string& op, const std::string& cond_val) {
    // Basic type inference: if both strings represent numbers, compare numerically
    try {
        int r_val = std::stoi(row_val);
        int c_val = std::stoi(cond_val);
        if (op == "=") return r_val == c_val;
        if (op == "<") return r_val < c_val;
        if (op == ">") return r_val > c_val;
    } catch (...) {
        // Fallback to string comparison
        if (op == "=") return row_val == cond_val;
        if (op == "<") return row_val < cond_val;
        if (op == ">") return row_val > cond_val;
    }
    return false;
}

void Executor::execute_select(const SelectQuery* query) {
    std::vector<std::string> columns;
    std::vector<std::vector<std::string>> rows;

    if (!storage.read_table(query->table_name, columns, rows)) {
        std::cerr << "Failed to read table '" << query->table_name << "'." << std::endl;
        return;
    }

    std::vector<int> col_indices;
    if (query->columns.size() == 1 && query->columns[0] == "*") {
        for (size_t i = 0; i < columns.size(); ++i) col_indices.push_back(i);
    } else {
        for (const auto& col_name : query->columns) {
            auto it = std::find(columns.begin(), columns.end(), col_name);
            if (it != columns.end()) {
                col_indices.push_back(std::distance(columns.begin(), it));
            } else {
                std::cerr << "Column '" << col_name << "' not found." << std::endl;
                return;
            }
        }
    }

    int where_col_idx = -1;
    if (query->has_where) {
        auto it = std::find(columns.begin(), columns.end(), query->where_clause.column);
        if (it != columns.end()) {
            where_col_idx = std::distance(columns.begin(), it);
        } else {
            std::cerr << "WHERE column '" << query->where_clause.column << "' not found." << std::endl;
            return;
        }
    }

    // Print headers
    for (size_t i = 0; i < col_indices.size(); ++i) {
        std::cout << columns[col_indices[i]];
        if (i < col_indices.size() - 1) std::cout << " | ";
    }
    std::cout << "\n";
    std::cout << std::string(col_indices.size() * 10, '-') << "\n";

    // Print rows
    int match_count = 0;
    for (const auto& row : rows) {
        if (query->has_where && where_col_idx != -1) {
            if (!evaluate_condition(row[where_col_idx], query->where_clause.op, query->where_clause.value)) {
                continue; 
            }
        }

        for (size_t i = 0; i < col_indices.size(); ++i) {
            std::cout << row[col_indices[i]];
            if (i < col_indices.size() - 1) std::cout << " | ";
        }
        std::cout << "\n";
        match_count++;
    }
    std::cout << "(" << match_count << " rows)\n";
}
