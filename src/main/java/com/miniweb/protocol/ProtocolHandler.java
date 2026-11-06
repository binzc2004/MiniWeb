package com.miniweb.protocol;

import java.net.Socket;

public interface ProtocolHandler {
    void handle(Socket socket) throws Exception;
}