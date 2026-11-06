
package com.miniweb.server;

import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MiniWebServerTest {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8080;

    @Test
    void shouldListenOnPort8080() throws Exception {
        try (Socket socket = new Socket(HOST, PORT)) {
            assertTrue(socket.isConnected());
        }
    }

    @Test
    void shouldAcceptFiveConnections() throws Exception {
        for (int i = 0; i < 5; i++) {
            try (Socket socket = new Socket(HOST, PORT)) {
                assertTrue(socket.isConnected());
            }
        }
    }

    @Test
    void shouldReceiveUtf8Message() throws Exception {
        String message = "你好，MiniWeb！";

        try (Socket socket = new Socket(HOST, PORT)) {
            OutputStream output = socket.getOutputStream();
            output.write(message.getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
    }

    @Test
    void shouldHandleClientDisconnect() throws Exception {
        try (Socket socket = new Socket(HOST, PORT)) {
            assertTrue(socket.isConnected());
        }

        try (Socket socket = new Socket(HOST, PORT)) {
            assertTrue(socket.isConnected());
        }
    }

    @Test
    void shouldKeepServerAvailable() throws Exception {
        try (Socket socket = new Socket(HOST, PORT)) {
            assertTrue(socket.isConnected());
        }
    }

    @Test
    void shouldKeepPortUnavailableWhileRunning() throws Exception {
        assertThrows(Exception.class, () -> {
            try (ServerSocket socket = new ServerSocket(PORT)) {
                // 如果能绑定，说明原服务器没有占用该端口
            }
        });
    }
}
