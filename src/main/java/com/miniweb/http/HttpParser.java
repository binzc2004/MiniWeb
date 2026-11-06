
package com.miniweb.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HttpParser {

    // 单行最大长度：8 KB
    private static final int MAX_LINE_SIZE = 8192;

    // 请求头最大总长度：32 KB
    private static final int MAX_HEADER_SIZE = 32768;

    // 请求体最大长度：10 MB
    private static final int MAX_BODY_SIZE = 10 * 1024 * 1024;

    /**
     * 解析一次 HTTP 请求。
     *
     * @return 解析完成的请求；如果读取请求行前连接已关闭，则返回 null
     */
    public HttpRequest parse(InputStream inputStream) throws IOException {

        // 1. 读取请求行
        String requestLine = readLine(inputStream, MAX_LINE_SIZE);

        if (requestLine == null) {
            return null;
        }

        String[] parts = requestLine.split(" ", -1);

        if (parts.length != 3
                || parts[0].isBlank()
                || parts[1].isBlank()
                || parts[2].isBlank()) {
            throw new HttpParseException("Invalid HTTP request line");
        }

        HttpMethod method;

        try {
            method = HttpMethod.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            throw new HttpParseException("Unsupported HTTP method: " + parts[0]);
        }

        String requestTarget = parts[1];
        String version = parts[2];

        if (!version.equals("HTTP/1.1") && !version.equals("HTTP/1.0")) {
            throw new HttpParseException("Unsupported HTTP version: " + version);
        }

        // 2. 读取请求头
        Map<String, String> headers = new HashMap<>();
        Map<String, String> normalizedHeaders = new HashMap<>();

        int totalHeaderSize = 0;

        while (true) {
            String line = readLine(inputStream, MAX_LINE_SIZE);

            if (line == null) {
                throw new HttpParseException("Unexpected EOF in HTTP headers");
            }

            totalHeaderSize += line.length() + 2;

            if (totalHeaderSize > MAX_HEADER_SIZE) {
                throw new HttpParseException("HTTP headers too large");
            }

            // 空行表示请求头结束
            if (line.isEmpty()) {
                break;
            }

            int separator = line.indexOf(':');

            if (separator <= 0) {
                throw new HttpParseException("Invalid HTTP header: " + line);
            }

            String name = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();

            if (!name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) {
                throw new HttpParseException("Invalid HTTP header name");
            }

            String normalizedName = name.toLowerCase(Locale.ROOT);

            // 第一版不支持重复请求头，避免 Map 静默覆盖数据
            if (normalizedHeaders.containsKey(normalizedName)) {
                throw new HttpParseException("Duplicate HTTP header: " + name);
            }

            headers.put(name, value);
            normalizedHeaders.put(normalizedName, value);
        }

        // 3. 处理请求体传输方式
        if (normalizedHeaders.containsKey("transfer-encoding")) {
            throw new HttpParseException(
                    "Transfer-Encoding is not supported yet"
            );
        }

        byte[] body = new byte[0];

        String contentLengthValue = normalizedHeaders.get("content-length");

        if (contentLengthValue != null) {
            long contentLength;

            try {
                contentLength = Long.parseLong(contentLengthValue);
            } catch (NumberFormatException e) {
                throw new HttpParseException("Invalid Content-Length");
            }

            if (contentLength < 0 || contentLength > MAX_BODY_SIZE) {
                throw new HttpParseException("Invalid or oversized request body");
            }

            // readNBytes 会循环读取，直到读满指定长度或遇到 EOF
            body = inputStream.readNBytes((int) contentLength);

            if (body.length != contentLength) {
                throw new HttpParseException("Incomplete HTTP request body");
            }
        }

        // 4. 解析 URL Path 和 Query 参数
        String path;
        Map<String, List<String>> queryParameters;

        try {
            java.net.URI uri = java.net.URI.create(requestTarget);

            if (!requestTarget.startsWith("/") && !requestTarget.equals("*")) {
                throw new HttpParseException("Unsupported request target");
            }

            if (uri.getRawFragment() != null) {
                throw new HttpParseException("Invalid request target fragment");
            }

            String rawPath = uri.getRawPath();

            path = rawPath.isEmpty() ? "/" : URLDecoder.decode(
                    rawPath.replace("+", "%2B"),
                    StandardCharsets.UTF_8
            );

            queryParameters = parseQuery(uri.getRawQuery());

        } catch (IllegalArgumentException e) {
            throw new HttpParseException("Invalid request target: " + requestTarget);
        }

        // 5. 构建 HttpRequest
        return new HttpRequest(
                method,
                path,
                version,
                headers,
                queryParameters,
                body
        );
    }

    /**
     * 按 CRLF 读取一行。
     */
    private String readLine(InputStream inputStream, int maxLength)
            throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        while (true) {
            int value = inputStream.read();

            if (value == -1) {
                if (buffer.size() == 0) {
                    return null;
                }

                throw new HttpParseException("Unexpected EOF while reading line");
            }

            if (value == '\r') {
                int next = inputStream.read();

                if (next != '\n') {
                    throw new HttpParseException("Invalid HTTP line ending");
                }

                return buffer.toString(StandardCharsets.ISO_8859_1);
            }

            if (value == '\n') {
                throw new HttpParseException("Invalid HTTP line ending");
            }

            buffer.write(value);

            if (buffer.size() > maxLength) {
                throw new HttpParseException("HTTP line too long");
            }
        }
    }

    /**
     * 解析 URL Query 参数。
     */
    private Map<String, List<String>> parseQuery(String rawQuery) {

        Map<String, List<String>> parameters = new HashMap<>();

        if (rawQuery == null || rawQuery.isEmpty()) {
            return parameters;
        }

        for (String pair : rawQuery.split("&", -1)) {
            int separator = pair.indexOf('=');

            String key = separator < 0
                    ? pair
                    : pair.substring(0, separator);

            String value = separator < 0
                    ? ""
                    : pair.substring(separator + 1);

            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);

            parameters.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(value);
        }

        return parameters;
    }
}
