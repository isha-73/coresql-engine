package com.coresql.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class CoreSQLClient {
    private final String host;
    private final int port;

    public CoreSQLClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try (SocketChannel client = SocketChannel.open(new InetSocketAddress(host, port));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to CoreSQL Server at " + host + ":" + port);
            System.out.println("Type SQL statements. Type 'EXIT' to quit.");

            while (true) {
                System.out.print("CoreSQL-Client> ");
                if (!scanner.hasNextLine()) break;

                String line = scanner.nextLine();
                if (line.trim().isEmpty()) continue;
                if (line.trim().equalsIgnoreCase("EXIT")) break;

                sendQuery(client, line);
                receiveResponse(client);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private void sendQuery(SocketChannel client, String query) throws IOException {
        byte[] payloadBytes = query.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(5 + payloadBytes.length);
        buffer.put((byte) 0x01); // QUERY
        buffer.putInt(payloadBytes.length);
        buffer.put(payloadBytes);
        buffer.flip();

        while (buffer.hasRemaining()) {
            client.write(buffer);
        }
    }

    private void receiveResponse(SocketChannel client) throws IOException {
        ByteBuffer headerBuffer = ByteBuffer.allocate(5);
        while (headerBuffer.hasRemaining()) {
            int read = client.read(headerBuffer);
            if (read == -1) throw new IOException("Server disconnected");
        }

        headerBuffer.flip();
        byte type = headerBuffer.get();
        int length = headerBuffer.getInt();

        if (length < 0 || length > 10 * 1024 * 1024) {
            System.err.println("Invalid payload length: " + length);
            return;
        }

        ByteBuffer payloadBuffer = ByteBuffer.allocate(length);
        while (payloadBuffer.hasRemaining()) {
            int read = client.read(payloadBuffer);
            if (read == -1) throw new IOException("Server disconnected");
        }

        payloadBuffer.flip();
        String payload = new String(payloadBuffer.array(), StandardCharsets.UTF_8);

        if (type == 0x03) {
            System.out.println(payload);
        } else if (type == 0x04) {
            System.err.println(payload);
        } else {
            System.out.println("Unknown response type (" + type + "): " + payload);
        }
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 5455;
        for (String arg : args) {
            if (arg.startsWith("--host=")) {
                host = arg.substring(7);
            } else if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring(7));
            }
        }
        new CoreSQLClient(host, port).start();
    }
}
