package com.miniweb.routing;

import com.miniweb.http.HttpMethod;
import com.miniweb.http.HttpRequest;
import com.miniweb.http.HttpResponse;
import com.miniweb.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 请求分发器。
 *
 * @author zicheng bin
 */
public class Dispatcher {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);

    private final Router router;

    public Dispatcher(Router router) {
        if (router == null) {
            throw new IllegalArgumentException("Router cannot be null");
        }

        this.router = router;
    }

    /**
     * 分发 HTTP 请求。
     *
     * @param request HTTP 请求
     * @return HTTP 响应
     */
    public HttpResponse dispatch(HttpRequest request) {
        log.debug("Dispatching request: {} {}", request.getMethod(), request.getPath());

        RouteMatch match = router.match(request);

        return switch (match.getStatus()) {
            case MATCHED -> handleMatched(request, match);
            case NOT_FOUND -> createResponse(
                    request,
                    HttpStatus.NOT_FOUND,
                    "Not Found");
            case METHOD_NOT_ALLOWED -> handleMethodNotAllowed(
                    request,
                    match);
        };
    }

    /**
     * 执行匹配成功的路由处理器。
     */
    private HttpResponse handleMatched(
            HttpRequest request,
            RouteMatch match) {
        RouteDefinition route = match.getRouteDefinition();

        try {
            log.debug(
                    "Matched route: {} {}",
                    route.getMethod(),
                    route.getPath());

            HttpResponse response = route.getHandler().handle(
                    request,
                    match.getPathVariables());

            if (response == null) {
                log.error("Route handler returned null response");
                return createResponse(
                        request,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error");
            }

            return response;

        } catch (Exception e) {
            log.error(
                    "Route handler execution failed: {} {}",
                    request.getMethod(),
                    request.getPath(),
                    e);

            return createResponse(
                    request,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal Server Error");
        }
    }

    /**
     * 处理请求方法不允许的情况。
     */
    private HttpResponse handleMethodNotAllowed(
            HttpRequest request,
            RouteMatch match) {
        String allowedMethods = match.getAllowedMethods()
                .stream()
                .map(HttpMethod::name)
                .sorted()
                .collect(Collectors.joining(", "));

        return new HttpResponse(
                request.getVersion(),
                HttpStatus.METHOD_NOT_ALLOWED,
                Map.of(
                        "Content-Type", "text/plain; charset=UTF-8",
                        "Allow", allowedMethods),
                "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建通用 HTTP 响应。
     */
    private HttpResponse createResponse(
            HttpRequest request,
            HttpStatus status,
            String message) {
        return new HttpResponse(
                request.getVersion(),
                status,
                Map.of("Content-Type", "text/plain; charset=UTF-8"),
                message.getBytes(StandardCharsets.UTF_8));
    }
}