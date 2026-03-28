cmake_minimum_required(VERSION 3.10)
project(CoreSQL VERSION 1.0)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)

# Add Include Directories
include_directories(src)

# Source files
set(SOURCES
    src/tokenizer/tokenizer.cpp
    src/parser/parser.cpp
    src/executor/executor.cpp
    src/storage/storage_engine.cpp
    src/main.cpp
)


# Executable
add_executable(CoreSQL ${SOURCES})
