
package com.miniweb.http;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpResponse {

    // HTTP 协议版本
    private final String version;

    // HTTP 状态码
    private final HttpStatus status;

    // HTTP 响应头
    private final Map<String, String> headers;

    // HTTP 响应体
    private final byte[] body;

    public HttpResponse(
            String version,
            HttpStatus status,
            Map<String, String> headers,
            byte[] body
    ) {
        this.version = version;
        this.status = status;
        this.headers = Map.copyOf(headers);
        this.body = body == null ? new byte[0] : body.clone();
    }

    // 获取 HTTP 协议版本
    public String getVersion() {
        return version;
    }

    // 获取 HTTP 状态码
    public HttpStatus getStatus() {
        return status;
    }

    // 获取全部响应头
    public Map<String, String> getHeaders() {
        return headers;
    }

    // 根据名称获取响应头，不区分大小写
    public String getHeader(String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    // 获取原始响应体
    public byte[] getBody() {
        return body.clone();
    }

    // 将响应体按照 UTF-8 编码转换为字符串
    public String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
