package com.coresql.server;

import com.coresql.ast.Query;
import com.coresql.engine.Executor;
import com.coresql.parser.Parser;
import com.coresql.tokenizer.Token;
import com.coresql.tokenizer.Tokenizer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ClientSession {
    private final SocketChannel client;
    private final Executor executor;

    public ClientSession(SocketChannel client, Executor executor) {
        this.client = client;
        this.executor = executor;
    }

    public void handle() {
        try {
            while (true) {
                // Read exactly 5 bytes for the header
                ByteBuffer headerBuffer = ByteBuffer.allocate(5);
                while (headerBuffer.hasRemaining()) {
                    int read = client.read(headerBuffer);
                    if (read == -1) return; // Client disconnected
                }

                headerBuffer.flip();
                byte type = headerBuffer.get();
                int length = headerBuffer.getInt();

                if (length < 0 || length > 10 * 1024 * 1024) { // 10MB limit
                    System.err.println("Invalid packet length: " + length);
                    break;
                }

                // Read payload
                ByteBuffer payloadBuffer = ByteBuffer.allocate(length);
                while (payloadBuffer.hasRemaining()) {
                    int read = client.read(payloadBuffer);
                    if (read == -1) return; // Client disconnected
                }

                payloadBuffer.flip();
                String payload = new String(payloadBuffer.array(), StandardCharsets.UTF_8);

                // Handle ping
                if (type == 0x05) { // PING
                    sendResponse((byte) 0x06, ""); // PONG
                    continue;
                }

                // Handle query
                if (type == 0x01) { // QUERY
                    String response = executeQuery(payload);
                    if (response.startsWith("Error")) {
                        sendResponse((byte) 0x04, response); // ERROR
                    } else {
                        sendResponse((byte) 0x03, response); // RESULT_DONE
                    }
                } else {
                    sendResponse((byte) 0x04, "Error: Unknown packet type.");
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected with error.");
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private String executeQuery(String queryStr) {
        if (queryStr.trim().isEmpty()) return "Error: Empty query.";
        
        try {
            Tokenizer tokenizer = new Tokenizer(queryStr);
            List<Token> tokens = tokenizer.tokenize();

            Parser parser = new Parser(tokens);
            Query ast = parser.parse();

            String result = executor.execute(ast);
            return result != null ? result : "Success";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private void sendResponse(byte type, String payload) throws IOException {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(5 + payloadBytes.length);
        buffer.put(type);
        buffer.putInt(payloadBytes.length);
        buffer.put(payloadBytes);
        buffer.flip();

        while (buffer.hasRemaining()) {
            client.write(buffer);
        }
    }
}
