#pragma once

#include <string>
#include <vector>

class StorageEngine {
private:
    std::string tables_directory = "tables/";

    std::string get_table_path(const std::string& table_name);

public:
    StorageEngine();

    // Creates a new table (CSV file) with the given columns
    bool create_table(const std::string& table_name, const std::vector<std::string>& columns);

    // Inserts a new row into the table
    bool insert_row(const std::string& table_name, const std::vector<std::string>& values);

    // Reads all rows from the table. Returns a pair of columns and rows.
    bool read_table(const std::string& table_name, 
                    std::vector<std::string>& out_columns,
                    std::vector<std::vector<std::string>>& out_rows);
};
