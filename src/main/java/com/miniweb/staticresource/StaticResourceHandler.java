package com.miniweb.staticresource;

import com.miniweb.http.HttpMethod;
import com.miniweb.http.HttpRequest;
import com.miniweb.http.HttpResponse;
import com.miniweb.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.miniweb.MiniWeb;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;

/**
 * 静态资源处理器。
 *
 * @author zicheng bin
 */
public class StaticResourceHandler {

    private static final Logger log = MiniWeb.getLogger(StaticResourceHandler.class);

    private final Path rootDirectory;
    private final MimeTypeResolver mimeTypeResolver;

    public StaticResourceHandler(String rootDirectory) {
        if (rootDirectory == null || rootDirectory.isBlank()) {
            throw new IllegalArgumentException(
                    "Static resource directory cannot be empty");
        }

        try {
            Path directory = Path.of(rootDirectory).toRealPath();

            if (!Files.isDirectory(directory)) {
                throw new IllegalArgumentException(
                        "Static resource path is not a directory");
            }

            this.rootDirectory = directory;
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Static resource directory does not exist", e);
        }

        this.mimeTypeResolver = new MimeTypeResolver();
    }

    /**
     * 处理静态资源请求。
     *
     * @param request HTTP 请求
     * @return HTTP 响应
     */
    public HttpResponse handle(HttpRequest request) {
        if (request.getMethod() != HttpMethod.GET) {
            return createResponse(
                    request,
                    HttpStatus.METHOD_NOT_ALLOWED,
                    "Method Not Allowed");
        }

        try {
            String requestPath = request.getPath();

            if (requestPath == null || requestPath.contains("\\")
                    || requestPath.contains("\0")) {
                return createResponse(
                        request,
                        HttpStatus.BAD_REQUEST,
                        "Bad Request");
            }

            String relativePath = requestPath.replaceFirst("^/+", "");

            Path target = rootDirectory.resolve(relativePath).normalize();

            if (!target.startsWith(rootDirectory)) {
                log.warn("Static resource path traversal rejected: {}",
                        requestPath);

                return createResponse(
                        request,
                        HttpStatus.FORBIDDEN,
                        "Forbidden");
            }

            if (Files.isDirectory(target)) {
                target = target.resolve("index.html");
            }

            if (!Files.exists(target)) {
                return createResponse(
                        request,
                        HttpStatus.NOT_FOUND,
                        "Not Found");
            }

            // 检查真实路径，防止通过符号链接访问根目录之外的文件
            Path realPath = target.toRealPath();

            if (!realPath.startsWith(rootDirectory)) {
                log.warn("Static resource access outside root rejected: {}",
                        requestPath);

                return createResponse(
                        request,
                        HttpStatus.FORBIDDEN,
                        "Forbidden");
            }

            if (!Files.isRegularFile(realPath) || !Files.isReadable(realPath)) {
                return createResponse(
                        request,
                        HttpStatus.NOT_FOUND,
                        "Not Found");
            }

            byte[] body = Files.readAllBytes(realPath);
            String contentType = mimeTypeResolver.resolve(
                    realPath.getFileName().toString());

            log.debug("Serving static resource: {}", requestPath);

            return new HttpResponse(
                    request.getVersion(),
                    HttpStatus.OK,
                    Map.of("Content-Type", contentType),
                    body);

        } catch (InvalidPathException e) {
            return createResponse(
                    request,
                    HttpStatus.BAD_REQUEST,
                    "Bad Request");
        } catch (IOException e) {
            log.error("Failed to read static resource: {}",
                    request.getPath(), e);

            return createResponse(
                    request,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error");
        }
    }

    private HttpResponse createResponse(
            HttpRequest request,
            HttpStatus status,
            String message) {
        return new HttpResponse(
                request.getVersion(),
                status,
                Map.of("Content-Type", "text/plain; charset=UTF-8"),
                message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}