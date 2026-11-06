package com.miniweb;

import com.miniweb.banner.MiniWebBanner;
import com.miniweb.http.HttpProtocolHandler;
import com.miniweb.protocol.ProtocolHandler;
import com.miniweb.server.MiniWebServer;

public class MiniWeb {
    public static void main(String[] args) {
        MiniWebBanner.print();
        String host = "127.0.0.1";
        int port = 8080;

        ProtocolHandler protocolHandler = new HttpProtocolHandler();

        MiniWebServer server = new MiniWebServer(
                host,
                port,
                protocolHandler
        );

        server.start();
    }
}