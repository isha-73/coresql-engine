#include "tokenizer/tokenizer.h"
#include "parser/parser.h"
#include "executor/executor.h"
#include "storage/storage_engine.h"
#include <iostream>
#include <string>

void print_welcome() {
    std::cout << "CoreSQL v1.0\n";
    std::cout << "Enter SQL statements. Type 'EXIT' or 'QUIT' to quit.\n";
}

int main() {
    print_welcome();

    StorageEngine storage;
    Executor executor(storage);

    std::string line;
    while (true) {
        std::cout << "CoreSQL> ";
        if (!std::getline(std::cin, line)) {
             break;
        }

        if (line.empty()) continue;

        std::string upper_line = line;
        for (char &c : upper_line) c = std::toupper(c);

        if (upper_line == "EXIT" || upper_line == "QUIT" || upper_line == "EXIT;" || upper_line == "QUIT;") {
            break;
        }

        try {
            Tokenizer tokenizer(line);
            std::vector<Token> tokens = tokenizer.tokenize();

            Parser parser(tokens);
            std::shared_ptr<Query> ast = parser.parse();

            executor.execute(ast);
        } catch (const std::exception& e) {
            std::cerr << "Error: " << e.what() << "\n";
        }
    }

    std::cout << "Bye!\n";
    return 0;
}
