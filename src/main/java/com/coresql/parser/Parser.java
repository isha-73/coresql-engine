package com.coresql.parser;

import com.coresql.ast.*;
import com.coresql.tokenizer.Token;
import com.coresql.tokenizer.TokenType;

import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token peek() {
        if (isEof()) return tokens.get(tokens.size() - 1);
        return tokens.get(pos);
    }

    private Token advance() {
        if (isEof()) return tokens.get(tokens.size() - 1);
        return tokens.get(pos++);
    }

    private boolean isEof() {
        return pos >= tokens.size() || tokens.get(pos).type() == TokenType.END_OF_FILE;
    }

    private boolean match(TokenType type, String value) {
        if (isEof()) return false;
        Token t = peek();
        if (t.type() == type && (value.isEmpty() || t.value().equals(value))) {
            advance();
            return true;
        }
        return false;
    }

    private boolean match(TokenType type) {
        return match(type, "");
    }

    private void expect(TokenType type, String value) {
        if (!match(type, value)) {
            String expected = value.isEmpty() ? "token type " + type : value;
            throw new IllegalArgumentException("Syntax error: expected " + expected + " but found " + peek().value());
        }
    }

    private void expect(TokenType type) {
        expect(type, "");
    }

    public Query parse() {
        Query query;
        if (match(TokenType.KEYWORD, "CREATE")) {
            query = parseCreateTable();
        } else if (match(TokenType.KEYWORD, "INSERT")) {
            query = parseInsert();
        } else if (match(TokenType.KEYWORD, "SELECT")) {
            query = parseSelect();
        } else {
            throw new IllegalArgumentException("Syntax error: unexpected token " + peek().value());
        }

        match(TokenType.SYMBOL, ";");

        if (!isEof()) {
            throw new IllegalArgumentException("Syntax error: trailing garbage at end of statement: " + peek().value());
        }

        return query;
    }

    private Query parseCreateTable() {
        CreateTableQuery query = new CreateTableQuery();
        
        expect(TokenType.KEYWORD, "TABLE");
        
        Token nameToken = advance();
        if (nameToken.type() != TokenType.IDENTIFIER) {
            throw new IllegalArgumentException("Syntax error: expected table name");
        }
        query.tableName = nameToken.value();
        
        expect(TokenType.SYMBOL, "(");
        
        // Parse columns
        while (!match(TokenType.SYMBOL, ")")) {
            Token colToken = advance();
            if (colToken.type() != TokenType.IDENTIFIER) {
                throw new IllegalArgumentException("Syntax error: expected column name");
            }

            Token typeToken = advance();
            if (typeToken.type() != TokenType.KEYWORD && typeToken.type() != TokenType.IDENTIFIER) {
                throw new IllegalArgumentException("Syntax error: expected column type");
            }

            query.columns.add(new ColumnDefinition(colToken.value(), typeToken.value()));
            
            if (!match(TokenType.SYMBOL, ",")) {
                expect(TokenType.SYMBOL, ")");
                break;
            }
        }
        
        return query;
    }

    private Query parseInsert() {
        InsertQuery query = new InsertQuery();
        
        expect(TokenType.KEYWORD, "INTO");
        
        Token nameToken = advance();
        if (nameToken.type() != TokenType.IDENTIFIER) {
            throw new IllegalArgumentException("Syntax error: expected table name");
        }
        query.tableName = nameToken.value();
        
        expect(TokenType.KEYWORD, "VALUES");
        expect(TokenType.SYMBOL, "(");
        
        // Parse values
        while (!match(TokenType.SYMBOL, ")")) {
            Token valToken = advance();
            if (valToken.type() == TokenType.STRING || valToken.type() == TokenType.INTEGER) {
                query.values.add(valToken.value());
            } else {
                throw new IllegalArgumentException("Syntax error: expected value (string or integer)");
            }
            
            if (!match(TokenType.SYMBOL, ",")) {
                expect(TokenType.SYMBOL, ")");
                break;
            }
        }
        
        return query;
    }

    private Query parseSelect() {
        SelectQuery query = new SelectQuery();
        
        // Parse columns
        Token colToken = advance();
        if (colToken.type() == TokenType.IDENTIFIER) {
            query.columns.add(colToken.value());
        } else if (colToken.type() == TokenType.SYMBOL && colToken.value().equals("*")) {
            query.columns.add("*"); 
        } else {
             throw new IllegalArgumentException("Syntax error: expected column name");
        }
        
        expect(TokenType.KEYWORD, "FROM");
        
        Token nameToken = advance();
        if (nameToken.type() != TokenType.IDENTIFIER) {
            throw new IllegalArgumentException("Syntax error: expected table name");
        }
        query.tableName = nameToken.value();
        
        if (match(TokenType.KEYWORD, "WHERE")) {
            query.hasWhere = true;
            query.whereClause = new Condition();
            
            Token whereCol = advance();
            if (whereCol.type() != TokenType.IDENTIFIER) throw new IllegalArgumentException("Syntax error: expected column in WHERE");
            query.whereClause.column = whereCol.value();
            
            Token opToken = advance();
            if (opToken.type() != TokenType.OPERATOR) throw new IllegalArgumentException("Syntax error: expected operator in WHERE");
            query.whereClause.op = opToken.value();
            
            Token valToken = advance();
            if (valToken.type() != TokenType.STRING && valToken.type() != TokenType.INTEGER) {
                throw new IllegalArgumentException("Syntax error: expected value in WHERE");
            }
            query.whereClause.value = valToken.value();
        }
        
        return query;
    }
}
