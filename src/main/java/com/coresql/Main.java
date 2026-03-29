package com.coresql;

import com.coresql.ast.Query;
import com.coresql.engine.Executor;
import com.coresql.engine.StorageEngine;
import com.coresql.parser.Parser;
import com.coresql.tokenizer.Token;
import com.coresql.tokenizer.Tokenizer;

import java.util.List;
import java.util.Scanner;

public class Main {
    private static void printWelcome() {
        System.out.println("CoreSQL v1.0 (Java Edition)");
        System.out.println("Enter SQL statements. Type 'EXIT' or 'QUIT' to quit.");
    }

    public static void main(String[] args) {
        printWelcome();

        StorageEngine storage = new StorageEngine();
        Executor executor = new Executor(storage);

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("CoreSQL> ");
                if (!scanner.hasNextLine())
                    break;

                String line = scanner.nextLine();
                if (line.trim().isEmpty())
                    continue;

                String upperLine = line.trim().toUpperCase();
                if (upperLine.equals("EXIT") || upperLine.equals("QUIT") ||
                        upperLine.equals("EXIT;") || upperLine.equals("QUIT;")) {
                    break;
                }

                try {
                    Tokenizer tokenizer = new Tokenizer(line); // line is query string
                    List<Token> tokens = tokenizer.tokenize();

                    Parser parser = new Parser(tokens);
                    Query ast = parser.parse();

                    executor.execute(ast);
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
        System.out.println("Bye!");
    }
}
