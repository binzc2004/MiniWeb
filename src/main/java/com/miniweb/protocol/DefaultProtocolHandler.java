package com.miniweb.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.miniweb.MiniWeb;
public class DefaultProtocolHandler implements ProtocolHandler {
    private static final Logger log = MiniWeb.getLogger(DefaultProtocolHandler.class);

    @Override
    public void handle(Socket socket) throws Exception {
        byte[] buffer = new byte[1024];
        int length;
        byte[] test = "你好，MiniWeb！".getBytes(StandardCharsets.UTF_8);
        String decoded = new String(test, StandardCharsets.UTF_8);

        System.out.println("UTF-8 TEST: " + decoded);
        while ((length = socket.getInputStream().read(buffer)) != -1) {
            byte[] received = java.util.Arrays.copyOf(buffer, length);

            log.info("Received bytes: {}",
                    java.util.Arrays.toString(received));

            String message = new String(received, StandardCharsets.UTF_8);

            log.info("Received message: {}", message);
            System.out.println(message);
        }
    }

}