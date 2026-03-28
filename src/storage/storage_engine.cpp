#include "storage_engine.h"
#include <fstream>
#include <iostream>
#include <sstream>

StorageEngine::StorageEngine() {
}

std::string StorageEngine::get_table_path(const std::string& table_name) {
    return tables_directory + table_name + ".csv";
}

bool StorageEngine::create_table(const std::string& table_name, const std::vector<std::string>& columns) {
    std::string path = get_table_path(table_name);
    // Check if exists
    std::ifstream check_file(path);
    if (check_file.good()) {
        std::cerr << "Table '" << table_name << "' already exists." << std::endl;
        return false;
    }

    std::ofstream file(path);
    if (!file.is_open()) {
        std::cerr << "Failed to create table file: " << path << std::endl;
        return false;
    }

    // Write header
    for (size_t i = 0; i < columns.size(); ++i) {
        file << columns[i];
        if (i < columns.size() - 1) {
            file << ",";
        }
    }
    file << "\n";
    file.close();
    return true;
}

bool StorageEngine::insert_row(const std::string& table_name, const std::vector<std::string>& values) {
    std::string path = get_table_path(table_name);
    std::ofstream file(path, std::ios::app);
    if (!file.is_open()) {
        std::cerr << "Table '" << table_name << "' does not exist." << std::endl;
        return false;
    }

    for (size_t i = 0; i < values.size(); ++i) {
        file << values[i];
        if (i < values.size() - 1) {
            file << ",";
        }
    }
    file << "\n";
    file.close();
    return true;
}

bool StorageEngine::read_table(const std::string& table_name, 
                               std::vector<std::string>& out_columns,
                               std::vector<std::vector<std::string>>& out_rows) {
    std::string path = get_table_path(table_name);
    std::ifstream file(path);
    if (!file.is_open()) {
        std::cerr << "Table '" << table_name << "' does not exist." << std::endl;
        return false;
    }

    std::string line;
    // Read header
    if (std::getline(file, line)) {
        std::stringstream ss(line);
        std::string col;
        while (std::getline(ss, col, ',')) {
            out_columns.push_back(col);
        }
    } else {
        return false; // Empty file
    }

    // Read rows
    while (std::getline(file, line)) {
        std::vector<std::string> row;
        std::stringstream ss(line);
        std::string val;
        while (std::getline(ss, val, ',')) {
            row.push_back(val);
        }
        out_rows.push_back(row);
    }

    file.close();
    return true;
}
