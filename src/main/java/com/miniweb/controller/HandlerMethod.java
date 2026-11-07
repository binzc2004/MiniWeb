
package com.miniweb.controller;

import com.miniweb.http.HttpRequest;
import com.miniweb.http.HttpResponse;
import com.miniweb.http.HttpStatus;
import com.miniweb.routing.RouteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.miniweb.MiniWeb;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Controller 方法处理器。
 *
 * 负责协调参数解析、反射调用和返回值处理。
 *
 * @author zicheng bin
 */
public class HandlerMethod implements RouteHandler {

    private static final Logger log = MiniWeb.getLogger(HandlerMethod.class);

    private final Object controller;
    private final Method method;
    private final ArgumentResolver argumentResolver;
    private final ReturnValueHandler returnValueHandler;

    public HandlerMethod(
            Object controller,
            Method method,
            ArgumentResolver argumentResolver,
            ReturnValueHandler returnValueHandler) {
        if (controller == null || method == null
                || argumentResolver == null || returnValueHandler == null) {
            throw new IllegalArgumentException(
                    "HandlerMethod dependencies cannot be null");
        }

        this.controller = controller;
        this.method = method;
        this.argumentResolver = argumentResolver;
        this.returnValueHandler = returnValueHandler;

        this.method.setAccessible(true);
    }

    /**
     * 处理 HTTP 请求并调用 Controller 方法。
     */
    @Override
    public HttpResponse handle(
            HttpRequest request,
            Map<String, String> pathVariables) {
        try {
            Object[] arguments = argumentResolver.resolveArguments(
                    method,
                    request,
                    pathVariables);

            log.debug(
                    "Invoking Controller method: {}.{}",
                    controller.getClass().getSimpleName(),
                    method.getName());

            Object result = method.invoke(controller, arguments);

            return returnValueHandler.handle(result);

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();

            log.error(
                    "Controller method execution failed: {}",
                    method.getName(),
                    cause);

            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (IllegalArgumentException e) {
            log.warn(
                    "Failed to resolve Controller arguments: {}",
                    e.getMessage());

            return createErrorResponse(HttpStatus.BAD_REQUEST);

        } catch (ReflectiveOperationException e) {
            log.error(
                    "Failed to invoke Controller method: {}",
                    method.getName(),
                    e);

            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 创建 HTTP 错误响应。
     */
    private HttpResponse createErrorResponse(HttpStatus status) {
        String body = status.getReasonPhrase();

        return new HttpResponse(
                "HTTP/1.1",
                status,
                Map.of("Content-Type", "text/plain; charset=UTF-8"),
                body.getBytes(StandardCharsets.UTF_8));
    }
}
