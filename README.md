# CoreSQL Engine (Java Edition)

CoreSQL is a completely custom, lightweight SQL Database Engine implemented entirely in Java 21 from scratch. It utilizes zero external database drivers or SQL parsing dependencies. It was built to demonstrate how a database compiles strings into storage-level operations.

## Project Architecture & Modules

The engine operates sequentially, translating raw text into AST representations and finally into hard file storage.

### 1. Tokenizer (`com.coresql.tokenizer`)
**Purpose:** Lexical Analysis.<br>
When you execute a text query (like `SELECT * FROM users`), the Tokenizer steps through character by character, identifying and grouping characters into meaningful `Token` objects. For example, `SELECT` becomes a `KEYWORD` token, `users` is an `IDENTIFIER`, and `*` is a `SYMBOL`. It strips whitespace and validates alphanumeric integrity so the downstream logic strictly handles clean logic blocks.

### 2. Parser (`com.coresql.parser`)
**Purpose:** Grammar Enforcement and AST creation.<br>
The Parser receives the ordered stream of `Token`s and enforces SQL syntax rules (e.g., ensuring `TABLE` is the second token in a `CREATE` statement). It parses these tokens into hierarchical Abstract Syntax Tree (AST) objects. If the syntax is invalid, it throws an `IllegalArgumentException`.

### 3. AST (Abstract Syntax Tree) (`com.coresql.ast`)
**Purpose:** Semantic Representation.<br>
This module is a collection of Java POJOs (Plain Old Java Objects) mimicking the structured logic of query intents: `CreateTableQuery`, `InsertQuery`, `SelectQuery`, and inner models like `Condition`. It provides strongly-typed parameters instead of passing hash maps around.

### 4. Storage Engine (`com.coresql.engine.StorageEngine`)
**Purpose:** Disk Persistent I/O.<br>
This component creates and manages a local `tables/` directory natively. Tables are stored efficiently as standard `.csv` files. The module acts strictly as the Data Access layer, utilizing purely standard `BufferedReader` and `BufferedWriter` to fetch and append lines of text without knowing what the queries actually are.

### 5. Executor (`com.coresql.engine.Executor`)
**Purpose:** Processing and Condition Evaluation.<br>
The Executor bridges the AST logic and the Storage Engine. Utilizing native Java 21 pattern-matching features, it intercepts `Query` objects and routes them. For `SelectQuery` operations, it pulls purely raw strings from the Storage Engine, loops through them, and handles strict data-type guessing and condition evaluation for `WHERE` clauses (e.g. `age > 25`).

---

## How to Execute

The application builds cleanly via Maven.

### Prerequisites
- JDK 21+ installed
- Apache Maven (`mvn`) installed

### 1. Terminal Execution
Open your terminal in the root directory and run the compilation lifecycle:
```console
$ mvn clean compile
```
Execute the REPL interface:
```console
$ mvn exec:java
```

### 2. IDE Execution
Because this repo uses a standard `pom.xml`, any major Java-enabled IDE (IntelliJ IDEA, Eclipse, Visual Studio Code) natively understands it.
- Open the project folder in your IDE.
- Locate `src/main/java/com/coresql/Main.java`.
- Click the native **Run** button to launch the console loop.

### 3. Usage Example
Once booted into the `CoreSQL>` prompt, try running the following sequential commands:

```sql
CREATE TABLE employees (id, name, department)
INSERT INTO employees VALUES (1, "Alice", "Engineering")
INSERT INTO employees VALUES (2, "Bob", "Sales")
SELECT * FROM employees
SELECT name FROM employees WHERE id = 1
EXIT
```
