#pragma once

#include <string>
#include <vector>

enum class TokenType {
    KEYWORD,      // SELECT, FROM, WHERE, CREATE, TABLE, INSERT, INTO, VALUES
    IDENTIFIER,   // table names, column names
    INTEGER,      // 123
    STRING,       // "hello"
    OPERATOR,     // =, >, <
    SYMBOL,       // (, ), ,
    END_OF_FILE,  // EOF
    UNKNOWN
};

struct Token {
    TokenType type;
    std::string value;
};

class Tokenizer {
public:
    Tokenizer(const std::string& query);
    std::vector<Token> tokenize();

private:
    std::string query;
    size_t pos;

    void skip_whitespace();
    Token read_number();
    Token read_string();
    Token read_identifier_or_keyword();
    Token read_operator_or_symbol();
    
    char peek() const;
    char advance();
    bool is_eof() const;
};
