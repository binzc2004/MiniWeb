package com.miniweb.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.miniweb.http.HttpProtocolHandler;
import com.miniweb.protocol.DefaultProtocolHandler;
import com.miniweb.protocol.ProtocolHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Handler;

public class MiniWebServer {

    private static final Logger log = LoggerFactory.getLogger(MiniWebServer.class);
    private final String host;
    private final int port;
    private ServerSocket serverSocket;
    //TODO:后续考虑handler怎么传入
    private ProtocolHandler protocolHandler;


    public MiniWebServer(String host, int port, ProtocolHandler protocolHandler) {
        this.host = host;
        this.port = port;
        this.protocolHandler = protocolHandler;
    }
    public MiniWebServer(int port) {
        this.port = port;
        this.host="0.0.0.0";
    }
    public MiniWebServer(String host,int port){
        this.host=host;
        this.port=port;
    }
    

    public void start() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(host, port));
            log.info("MiniWeb server started on {}:{}", host, port);
            log.info("MiniWeb server started on port {}", port);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                log.info("Client connected: {}", clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket, protocolHandler);
                new Thread(handler).start();
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
        }
    }
}
