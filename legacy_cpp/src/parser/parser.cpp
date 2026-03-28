#include "parser.h"
#include "../tokenizer/tokenizer.h"
#include <iostream>
#include <stdexcept>

Parser::Parser(const std::vector<Token> &tokens) : tokens(tokens), pos(0) {}

const Token &Parser::peek() const {
  if (is_eof())
    return tokens.back();
  return tokens[pos];
}

const Token &Parser::advance() {
  if (is_eof())
    return tokens.back();
  return tokens[pos++];
}

bool Parser::is_eof() const {
  return pos >= tokens.size() || tokens[pos].type == TokenType::END_OF_FILE;
}

bool Parser::match(TokenType type, const std::string &value) {
  if (is_eof())
    return false;
  const Token &t = peek();
  if (t.type == type && (value.empty() || t.value == value)) {
    advance();
    return true;
  }
  return false;
}

void Parser::expect(TokenType type, const std::string &value) {
  if (!match(type, value)) {
    std::string expected =
        value.empty() ? "token type " + std::to_string((int)type) : value;
    throw std::runtime_error("Syntax error: expected " + expected +
                             " but found " + peek().value);
  }
}

std::shared_ptr<Query> Parser::parse() {
  if (match(TokenType::KEYWORD, "CREATE")) {
    return parse_create_table();
  } else if (match(TokenType::KEYWORD, "INSERT")) {
    return parse_insert();
  } else if (match(TokenType::KEYWORD, "SELECT")) {
    return parse_select();
  } else {
    throw std::runtime_error("Syntax error: unexpected token " + peek().value);
  }
}

// CREATE TABLE <table_name> (<column1>, <column2>, ...)
std::shared_ptr<Query> Parser::parse_create_table() {
  auto query = std::make_shared<CreateTableQuery>();

  expect(TokenType::KEYWORD, "TABLE");

  const Token &name_token = advance();
  if (name_token.type != TokenType::IDENTIFIER) {
    throw std::runtime_error("Syntax error: expected table name");
  }
  query->table_name = name_token.value;

  expect(TokenType::SYMBOL, "(");

  // Parse columns
  while (!match(TokenType::SYMBOL, ")")) {
    const Token &col_token = advance();
    if (col_token.type != TokenType::IDENTIFIER) {
      throw std::runtime_error("Syntax error: expected column name");
    }
    query->columns.push_back(col_token.value);

    if (!match(TokenType::SYMBOL, ",")) {
      expect(TokenType::SYMBOL, ")");
      break;
    }
  }

  return query;
}

// INSERT INTO <table_name> VALUES (<value1>, <value2>, ...)
std::shared_ptr<Query> Parser::parse_insert() {
  auto query = std::make_shared<InsertQuery>();

  expect(TokenType::KEYWORD, "INTO");

  const Token &name_token = advance();
  if (name_token.type != TokenType::IDENTIFIER) {
    throw std::runtime_error("Syntax error: expected table name");
  }
  query->table_name = name_token.value;

  expect(TokenType::KEYWORD, "VALUES");
  expect(TokenType::SYMBOL, "(");

  // Parse values
  while (!match(TokenType::SYMBOL, ")")) {
    const Token &val_token = advance();
    if (val_token.type == TokenType::STRING ||
        val_token.type == TokenType::INTEGER) {
      query->values.push_back(val_token.value);
    } else {
      throw std::runtime_error(
          "Syntax error: expected value (string or integer)");
    }

    if (!match(TokenType::SYMBOL, ",")) {
      expect(TokenType::SYMBOL, ")");
      break;
    }
  }

  return query;
}

// SELECT <column_name> FROM <table_name> [WHERE <column_name> <operator>
// <value>]
std::shared_ptr<Query> Parser::parse_select() {
  auto query = std::make_shared<SelectQuery>();

  // Parse columns
  const Token &col_token = advance();
  if (col_token.type == TokenType::IDENTIFIER) {
    query->columns.push_back(col_token.value);
  } else if (col_token.type == TokenType::SYMBOL && col_token.value == "*") {
    query->columns.push_back("*");
  } else {
    throw std::runtime_error("Syntax error: expected column name");
  }

  expect(TokenType::KEYWORD, "FROM");

  const Token &name_token = advance();
  if (name_token.type != TokenType::IDENTIFIER) {
    throw std::runtime_error("Syntax error: expected table name");
  }
  query->table_name = name_token.value;

  if (match(TokenType::KEYWORD, "WHERE")) {
    query->has_where = true;

    const Token &where_col = advance();
    if (where_col.type != TokenType::IDENTIFIER)
      throw std::runtime_error("Syntax error: expected column in WHERE");
    query->where_clause.column = where_col.value;

    const Token &op_token = advance();
    if (op_token.type != TokenType::OPERATOR)
      throw std::runtime_error("Syntax error: expected operator in WHERE");
    query->where_clause.op = op_token.value;

    const Token &val_token = advance();
    if (val_token.type != TokenType::STRING &&
        val_token.type != TokenType::INTEGER) {
      throw std::runtime_error("Syntax error: expected value in WHERE");
    }
    query->where_clause.value = val_token.value;
  }

  return query;
}
