package com.miniweb.http;

public enum HttpStatus {

    // 2xx：请求成功
    OK(200, "OK"),
    CREATED(201, "Created"),
    NO_CONTENT(204, "No Content"),

    // 3xx：重定向
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),

    // 4xx：客户端请求错误
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    CONTENT_TOO_LARGE(413, "Content Too Large"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),

    // 5xx：服务器处理错误
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable");

    // HTTP 状态码
    private final int code;

    // HTTP 状态码对应的英文描述
    private final String reasonPhrase;

    // 构造状态码枚举
    HttpStatus(int code, String reasonPhrase) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }

    // 获取 HTTP 状态码
    public int getCode() {
        return code;
    }

    // 获取 HTTP 状态描述
    public String getReasonPhrase() {
        return reasonPhrase;
    }
}