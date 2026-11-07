
package com.miniweb.http;

import com.miniweb.protocol.ProtocolHandler;
import com.miniweb.routing.Dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpProtocolHandler implements ProtocolHandler {

    private static final Logger log =
            LoggerFactory.getLogger(HttpProtocolHandler.class);

    private final HttpParser parser;
    private final Dispatcher dispatcher;
    public HttpProtocolHandler(Dispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("Dispatcher cannot be null");
        }

        this.parser = new HttpParser();
        this.dispatcher = dispatcher;
    }

    @Override
    public void handle(Socket socket) throws IOException {

        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();

        while (true) {

            // 1. 解析一个 HTTP 请求
            HttpRequest request = parser.parse(input);

            // 客户端关闭连接，没有新的请求
            if (request == null) {
                break;
            }

            log.info("Received HTTP request: {} {}",
                    request.getMethod(), request.getPath());

            // 2. 请求分发
            HttpResponse response=this.dispatcher.dispatch(request);

        

            // 4. 根据 HTTP 版本和 Connection 请求头判断是否保持连接
            boolean keepAlive = shouldKeepAlive(request);

            // 5. 发送响应
            writeResponse(output, response, keepAlive);

            // 6. 如果不保持连接，结束处理
            if (!keepAlive) {
                break;
            }
        }
    }

    /**
     * 测试 Hook：后续由 Dispatcher 替换。
     */
    private HttpResponse testResponseHook(HttpRequest request) {
        byte[] body = ("Hello MiniWeb! You requested: " + request.getPath())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return new HttpResponse(
                "HTTP/1.1",
                HttpStatus.OK,
                java.util.Map.of("Content-Type", "text/plain; charset=UTF-8"),
                body
        );
    }

    /**
     * 判断是否保持 HTTP 连接。
     */
    private boolean shouldKeepAlive(HttpRequest request) {

        String connection = request.getHeader("Connection");

        if ("close".equalsIgnoreCase(connection)) {
            return false;
        }

        if ("keep-alive".equalsIgnoreCase(connection)) {
            return true;
        }

        // HTTP/1.1 默认持久连接
        return "HTTP/1.1".equals(request.getVersion());
    }

    /**
     * 发送 HTTP 响应。
     */
    private void writeResponse(
            OutputStream output,
            HttpResponse response,
            boolean keepAlive
    ) throws IOException {

        byte[] body = response.getBody();

        StringBuilder header = new StringBuilder();

        header.append(response.getVersion())
                .append(" ")
                .append(response.getStatus().getCode())
                .append(" ")
                .append(response.getStatus().getReasonPhrase())
                .append("\r\n");

        response.getHeaders().forEach((name, value) ->
                header.append(name)
                        .append(": ")
                        .append(value)
                        .append("\r\n")
        );

        header.append("Content-Length: ")
                .append(body.length)
                .append("\r\n");

        header.append("Connection: ")
                .append(keepAlive ? "keep-alive" : "close")
                .append("\r\n");

        header.append("\r\n");

        output.write(header.toString().getBytes(
                java.nio.charset.StandardCharsets.ISO_8859_1
        ));

        output.write(body);
        output.flush();
    }
}
