
package com.miniweb.controller;

import com.miniweb.http.HttpResponse;
import com.miniweb.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.miniweb.MiniWeb;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Controller 返回值处理器。
 *
 * @author zicheng bin
 */
public class ReturnValueHandler {

    private static final Logger log = MiniWeb.getLogger(ReturnValueHandler.class);

    private final JsonConverter jsonConverter;

    public ReturnValueHandler(JsonConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
    }

    /**
     * 将 Controller 返回值转换为 HTTP 响应。
     */
    public HttpResponse handle(Object result) {
        if (result == null) {
            log.debug("Controller returned null");

            return new HttpResponse(
                    "HTTP/1.1",
                    HttpStatus.NO_CONTENT,
                    Map.of(),
                    new byte[0]);
        }

        String body;
        String contentType;

        if (result instanceof String) {
            body = (String) result;
            contentType = "text/plain; charset=UTF-8";
        } else {
            body = jsonConverter.toJson(result);
            contentType = "application/json; charset=UTF-8";
        }

        log.debug(
                "Handling Controller return value: {}",
                result.getClass().getName());

        return new HttpResponse(
                "HTTP/1.1",
                HttpStatus.OK,
                Map.of("Content-Type", contentType),
                body.getBytes(StandardCharsets.UTF_8));
    }
}
