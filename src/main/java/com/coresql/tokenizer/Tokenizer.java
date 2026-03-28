package com.coresql.tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Tokenizer {
    private static final Set<String> KEYWORDS = Set.of(
        "CREATE", "TABLE", "INSERT", "INTO", "VALUES", "SELECT", "FROM", "WHERE"
    );

    private final String query;
    private int pos = 0;

    public Tokenizer(String query) {
        this.query = query;
    }

    private char peek() {
        if (isEof()) return '\0';
        return query.charAt(pos);
    }

    private char advance() {
        if (isEof()) return '\0';
        return query.charAt(pos++);
    }

    private boolean isEof() {
        return pos >= query.length();
    }

    private void skipWhitespace() {
        while (!isEof() && Character.isWhitespace(peek())) {
            advance();
        }
    }

    private Token readNumber() {
        StringBuilder val = new StringBuilder();
        while (!isEof() && Character.isDigit(peek())) {
            val.append(advance());
        }
        return new Token(TokenType.INTEGER, val.toString());
    }

    private Token readString() {
        advance(); // skip opening quote
        StringBuilder val = new StringBuilder();
        while (!isEof() && peek() != '"' && peek() != '\'') {
            val.append(advance());
        }
        if (!isEof()) advance(); // skip closing quote
        return new Token(TokenType.STRING, val.toString());
    }

    private Token readIdentifierOrKeyword() {
        StringBuilder val = new StringBuilder();
        while (!isEof() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            val.append(advance());
        }
        
        String upperVal = val.toString().toUpperCase();
        if (KEYWORDS.contains(upperVal)) {
            return new Token(TokenType.KEYWORD, upperVal);
        }
        return new Token(TokenType.IDENTIFIER, val.toString());
    }

    private Token readOperatorOrSymbol() {
        char c = advance();
        if (c == '=' || c == '<' || c == '>') {
            return new Token(TokenType.OPERATOR, String.valueOf(c));
        } else if (c == '(' || c == ')' || c == ',' || c == ';' || c == '*') {
            return new Token(TokenType.SYMBOL, String.valueOf(c));
        }
        return new Token(TokenType.UNKNOWN, String.valueOf(c));
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        
        while (!isEof()) {
            skipWhitespace();
            if (isEof()) break;

            char c = peek();
            if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdentifierOrKeyword());
            } else if (Character.isDigit(c)) {
                tokens.add(readNumber());
            } else if (c == '"' || c == '\'') {
                tokens.add(readString());
            } else {
                tokens.add(readOperatorOrSymbol());
            }
        }
        tokens.add(new Token(TokenType.END_OF_FILE, ""));
        return tokens;
    }
}
