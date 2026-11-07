
package com.miniweb.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.miniweb.MiniWeb;
import com.miniweb.protocol.ProtocolHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MiniWebServer {

    private static final Logger log = MiniWeb.getLogger(MiniWebServer.class);

    private static final int THREAD_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 100;

    private final String host;
    private final int port;
    private final ProtocolHandler protocolHandler;
    private final ThreadPoolExecutor executorService;

    private volatile ServerSocket serverSocket;

    public MiniWebServer(String host, int port, ProtocolHandler protocolHandler) {
        this.host = host;
        this.port = port;
        this.protocolHandler = Objects.requireNonNull(protocolHandler);

        this.executorService = new ThreadPoolExecutor(
                THREAD_POOL_SIZE,
                THREAD_POOL_SIZE,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.AbortPolicy());
    }

    public void start() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(host, port));

            log.info("MiniWeb server started on {}:{}", host, port);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();

                log.debug("Client connected: {}",
                        clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(
                        clientSocket,
                        protocolHandler);

                try {
                    executorService.execute(handler);
                } catch (RejectedExecutionException e) {
                    log.warn("Connection rejected: {}",
                            clientSocket.getRemoteSocketAddress());

                    try {
                        clientSocket.close();
                    } catch (IOException closeException) {
                        log.debug("Failed to close rejected connection",
                                closeException);
                    }
                }
            }

        } catch (IOException e) {
            if (serverSocket == null || !serverSocket.isClosed()) {
                log.error("Failed to run MiniWeb server", e);
            }
        }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                log.info("MiniWeb server stopped");
            }
        } catch (IOException e) {
            log.error("Failed to stop MiniWeb server", e);
        } finally {
            executorService.shutdown();
        }
    }
}
