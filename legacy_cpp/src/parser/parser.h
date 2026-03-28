#pragma once

#include "../ast/query_ast.h"
#include "../tokenizer/tokenizer.h"
#include <memory>
#include <vector>

class Parser {
public:
  Parser(const std::vector<Token> &tokens);

  // Parses the token list and returns a specific AST Query object
  std::shared_ptr<Query> parse();

private:
  std::vector<Token> tokens;
  size_t pos;

  const Token &peek() const;
  const Token &advance();
  bool is_eof() const;
  bool match(TokenType type, const std::string &value = "");
  void expect(TokenType type, const std::string &value = "");

  std::shared_ptr<Query> parse_create_table();
  std::shared_ptr<Query> parse_insert();
  std::shared_ptr<Query> parse_select();
};
