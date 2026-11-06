package com.miniweb.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.miniweb.protocol.ProtocolHandler;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private final ProtocolHandler protocolHandler;

    public ClientHandler(Socket socket, ProtocolHandler protocolHandler) {
        this.socket = socket;
        this.protocolHandler = protocolHandler;
    }

    @Override
    public void run() {
        try (socket) {
            log.debug("Handling client connection: {}",
                    socket.getRemoteSocketAddress());

            protocolHandler.handle(socket);

        } catch (IOException e) {
            log.warn("Client connection closed: {}",
                    socket.getRemoteSocketAddress());
        } catch (Exception e) {
            log.error("Failed to handle client connection", e);
        } finally {
            log.debug("Socket connection closed: {}",
                    socket.getRemoteSocketAddress());
        }
    }
}