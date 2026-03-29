CREATE TABLE products (id INT, name STRING, price INT, category STRING);

INSERT INTO products VALUES (1, "Laptop", 1200, "Electronics");
INSERT INTO products VALUES (2, "Desk", 300, "Furniture");
INSERT INTO products VALUES (3, "Mouse", 25, "Electronics");

INSERT INTO products VALUES ("four", "Keyboard", 50, "Electronics");
INSERT INTO products VALUES (5, "Monitor", "two hundred", "Electronics");

SELECT * FROM products;
SELECT name, price FROM products WHERE category = "Electronics";
SELECT * FROM products WHERE price > 100;

SELECT * FROM products WHERE price = "cheap";

EXIT;
