
package com.miniweb.http;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class HttpRequest {

    // HTTP 请求方法
    private final HttpMethod method;

    // URL 路径，不包含 Query 参数
    private final String path;

    // HTTP 协议版本
    private final String version;

    // HTTP 请求头
    private final Map<String, String> headers;

    // URL 查询参数，支持同名参数
    private final Map<String, List<String>> queryParameters;

    // HTTP 请求体，使用字节数组保存
    private final byte[] body;

    public HttpRequest(
            HttpMethod method,
            String path,
            String version,
            Map<String, String> headers,
            Map<String, List<String>> queryParameters,
            byte[] body
    ) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.headers = Map.copyOf(headers);
        this.queryParameters = Map.copyOf(queryParameters);
        this.body = body == null ? new byte[0] : body.clone();
    }

    // 获取 HTTP 请求方法
    public HttpMethod getMethod() {
        return method;
    }

    // 获取 URL 路径
    public String getPath() {
        return path;
    }

    // 获取 HTTP 协议版本
    public String getVersion() {
        return version;
    }

    // 获取全部请求头
    public Map<String, String> getHeaders() {
        return headers;
    }

    // 根据名称获取请求头，不区分大小写
    public String getHeader(String name) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    // 获取全部 URL 查询参数
    public Map<String, List<String>> getQueryParameters() {
        return queryParameters;
    }

    // 根据名称获取 URL 查询参数
    public List<String> getQueryParameter(String name) {
        return queryParameters.getOrDefault(name, List.of());
    }

    // 获取原始请求体
    public byte[] getBody() {
        return body.clone();
    }

    // 将请求体按照 UTF-8 编码转换为字符串
    public String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }
}
