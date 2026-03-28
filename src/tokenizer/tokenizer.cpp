#include "tokenizer.h"
#include <cctype>
#include <unordered_set>
#include <algorithm>

const std::unordered_set<std::string> KEYWORDS = {
    "CREATE", "TABLE", "INSERT", "INTO", "VALUES", "SELECT", "FROM", "WHERE"
};

Tokenizer::Tokenizer(const std::string& query) : query(query), pos(0) {}

char Tokenizer::peek() const {
    if (is_eof()) return '\0';
    return query[pos];
}

char Tokenizer::advance() {
    if (is_eof()) return '\0';
    return query[pos++];
}

bool Tokenizer::is_eof() const {
    return pos >= query.length();
}

void Tokenizer::skip_whitespace() {
    while (!is_eof() && std::isspace(peek())) {
        advance();
    }
}

Token Tokenizer::read_number() {
    std::string val = "";
    while (!is_eof() && std::isdigit(peek())) {
        val += advance();
    }
    return {TokenType::INTEGER, val};
}

Token Tokenizer::read_string() {
    advance(); // skip opening quote
    std::string val = "";
    while (!is_eof() && peek() != '"' && peek() != '\'') {
        val += advance();
    }
    if (!is_eof()) advance(); // skip closing quote
    return {TokenType::STRING, val};
}

Token Tokenizer::read_identifier_or_keyword() {
    std::string val = "";
    while (!is_eof() && (std::isalnum(peek()) || peek() == '_')) {
        val += advance();
    }
    
    // Check if keyword (case-insensitive comparison using uppercase)
    std::string upper_val = val;
    std::transform(upper_val.begin(), upper_val.end(), upper_val.begin(), ::toupper);
    
    if (KEYWORDS.find(upper_val) != KEYWORDS.end()) {
        return {TokenType::KEYWORD, upper_val};
    }
    return {TokenType::IDENTIFIER, val};
}

Token Tokenizer::read_operator_or_symbol() {
    char c = advance();
    if (c == '=' || c == '<' || c == '>') {
        return {TokenType::OPERATOR, std::string(1, c)};
    } else if (c == '(' || c == ')' || c == ',' || c == ';') {
        return {TokenType::SYMBOL, std::string(1, c)};
    }
    return {TokenType::UNKNOWN, std::string(1, c)};
}

std::vector<Token> Tokenizer::tokenize() {
    std::vector<Token> tokens;
    
    while (!is_eof()) {
        skip_whitespace();
        if (is_eof()) break;

        char c = peek();
        if (std::isalpha(c) || c == '_') {
            tokens.push_back(read_identifier_or_keyword());
        } else if (std::isdigit(c)) {
            tokens.push_back(read_number());
        } else if (c == '"' || c == '\'') {
            tokens.push_back(read_string());
        } else {
            tokens.push_back(read_operator_or_symbol());
        }
    }
    tokens.push_back({TokenType::END_OF_FILE, ""});
    return tokens;
}
