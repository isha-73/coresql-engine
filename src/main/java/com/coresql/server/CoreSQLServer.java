package com.coresql.server;

import com.coresql.engine.Executor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class CoreSQLServer {
    private final int port;
    private final Executor executor;
    private boolean running = false;

    public CoreSQLServer(int port, Executor executor) {
        this.port = port;
        this.executor = executor;
    }

    public void start() {
        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress(port));
            System.out.println("CoreSQL TCP Server started on port " + port);
            running = true;

            while (running) {
                SocketChannel client = server.accept();
                System.out.println("Accepted connection from " + client.getRemoteAddress());
                
                // Project Loom: Virtual Threads (Java 21)
                Thread.ofVirtual().start(() -> {
                    ClientSession session = new ClientSession(client, executor);
                    session.handle();
                });
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
