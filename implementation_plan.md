# CoreSQL – A Lightweight SQL Database Engine in C++

This document details the implementation plan for CoreSQL, a minimal relational database engine written in C++ that can parse and execute a subset of SQL queries. The goal is to build a highly modular architecture similar to real database engines, using only pure C++ (STL) without external database dependencies.

## User Review Required

> [!IMPORTANT]
> Please review the proposed architecture and project structure before we begin implementation. Once approved, I will proceed to create the project components systematically.

> [!WARNING]
> Since standard C++ does not provide a built-in cross-platform advanced file locking mechanism (without external dependencies or OS specific API), table storage via file modifications might not be highly concurrent. Let me know if you would like adding mutexes/locks in memory representation.

## Proposed Changes

The project will follow a 5-phase development lifecycle. We will break down the CoreSQL application into distinct components.

---

### Project Skeleton and Build System

Setting up the main directory structure and compilation instructions (We will use CMake or a simple generic build script).

#### [NEW] [CMakeLists.txt](file:///d:/Projects/coresql-engine/CMakeLists.txt)
#### [NEW] [README.md](file:///d:/Projects/coresql-engine/README.md)
#### [NEW] [main.cpp](file:///d:/Projects/coresql-engine/src/main.cpp)

---

### Tokenizer (Lexical Analyzer)

Will convert raw SQL strings into discrete Tokens (Keyword, Identifier, Operator, Literal).

#### [NEW] [tokenizer.h](file:///d:/Projects/coresql-engine/src/tokenizer/tokenizer.h)
#### [NEW] [tokenizer.cpp](file:///d:/Projects/coresql-engine/src/tokenizer/tokenizer.cpp)

---

### Abstract Syntax Tree (AST)

Definitions for the structured representation of SQL queries (CreateTableQuery, InsertQuery, SelectQuery). Includes enums for data types and operators.

#### [NEW] [query_ast.h](file:///d:/Projects/coresql-engine/src/ast/query_ast.h)

---

### Parser

Consumes tokens from the Lexer and matches them against our supported grammar to produce the Abstract Syntax Tree representations.

#### [NEW] [parser.h](file:///d:/Projects/coresql-engine/src/parser/parser.h)
#### [NEW] [parser.cpp](file:///d:/Projects/coresql-engine/src/parser/parser.cpp)

---

### Storage Layer

Manages file I/O for table definitions. Handles creating CSV files for tables, appending rows, and reading all rows.

#### [NEW] [storage_engine.h](file:///d:/Projects/coresql-engine/src/storage/storage_engine.h)
#### [NEW] [storage_engine.cpp](file:///d:/Projects/coresql-engine/src/storage/storage_engine.cpp)

---

### Execution Engine

Consumes the query ASTs and interacts with the Storage layer to fulfill instructions. Filters results using the given WHERE conditions.

#### [NEW] [executor.h](file:///d:/Projects/coresql-engine/src/executor/executor.h)
#### [NEW] [executor.cpp](file:///d:/Projects/coresql-engine/src/executor/executor.cpp)

---

## Open Questions

> [!IMPORTANT]
> * **Build System**: Are you okay with me setting up a `CMakeLists.txt` file for easy compilation?
> * **Data Types**: The schema example doesn't strictly define data types (e.g. `CREATE TABLE students (id, name, marks)`). Should the storage engine auto-infer types, store everything as strings internally until checked computationally, or require explicit types in table creation?

## Verification Plan

### Automated Tests
- No formal unit test library is requested using C++ standard libraries alone, but `main.cpp` will execute predefined test suites dynamically constructing statements and verifying results visually in the CLI.

### Manual Verification
- We will dynamically run test queries interactively through a REPL style loop using `main.cpp` and verifying that:
  - Output is correct for `SELECT`.
  - Proper `.csv` files are generated in the `tables/` directory.
