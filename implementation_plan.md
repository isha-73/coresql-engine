# CoreSQL – A Lightweight SQL Database Engine in Java

This document details the architecture and implementation of CoreSQL, a minimal relational database engine fully migrated to Java (targeting Java 21). It parses and executes a subset of SQL queries and implements a clean, generic, and modular architecture mirroring real engine layers without relying on external database dependencies.

## Architecture

The project follows a standard Maven directory structure (`src/main/java/com/coresql/`) and is broken down into distinct logical components.

---

### Build System & Entry Point

The application relies on Maven for dependency-free compilation to Java 21. `Main.java` serves as the interactive REPL shell for the database.

#### [pom.xml](file:///d:/Projects/coresql-engine/pom.xml)
#### [Main.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/Main.java)

---

### Tokenizer (Lexical Analyzer)

Converts raw SQL strings into discrete Java `Token` records (Keyword, Identifier, Operator, Literal).

#### [TokenType.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/tokenizer/TokenType.java)
#### [Token.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/tokenizer/Token.java)
#### [Tokenizer.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/tokenizer/Tokenizer.java)

---

### Abstract Syntax Tree (AST)

Definitions for the structured representation of SQL queries. Includes `Condition` models for `WHERE` clauses and generic extensions of the abstract `Query` class.

#### [Query.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/ast/Query.java)
#### [QueryType.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/ast/QueryType.java)
#### [CreateTableQuery.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/ast/CreateTableQuery.java)
#### [InsertQuery.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/ast/InsertQuery.java)
#### [SelectQuery.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/ast/SelectQuery.java)
#### [Condition.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/ast/Condition.java)

---

### Parser

Consumes tokens from the Tokenizer and matches them against the supported SQL grammar to produce the AST representations. Uses strict exception throwing for syntax rules.

#### [Parser.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/parser/Parser.java)

---

### Execution Engine

Consumes the query ASTs and interacts with the Storage logic to fulfill instructions. It leverages modern Java 21 enhanced `switch` pattern matching to route and execute logic and filter rows via `WHERE` condition evaluations.

#### [Executor.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/engine/Executor.java)

---

### Storage Layer

Manages file I/O for table definitions. Handles creating native `.csv` files for tables inside the `tables/` directory, cleanly appending rows natively, and reading datasets via generic `BufferedReader` operations.

#### [StorageEngine.java](file:///d:/Projects/coresql-engine/src/main/java/com/coresql/engine/StorageEngine.java)

---

## Legacy Codebase

> [!NOTE]  
> The original C++ implementation of CoreSQL has been preserved within the `legacy_cpp/` directory for historical tracking and structural comparisons.
