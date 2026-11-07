package com.miniweb;

import com.miniweb.http.HttpMethod;
import com.miniweb.http.HttpProtocolHandler;
import com.miniweb.http.HttpRequest;
import com.miniweb.http.HttpResponse;
import com.miniweb.http.HttpStatus;
import com.miniweb.protocol.ProtocolHandler;
import com.miniweb.routing.Dispatcher;
import com.miniweb.routing.Router;
import com.miniweb.server.MiniWebServer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * MiniWeb 应用启动入口。
 *
 * @author zicheng bin
 */
public class MiniWeb {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8080;

        Router router = new Router();

        // 开发阶段示例路由注册
        registerExampleRoutes(router);

        Dispatcher dispatcher = new Dispatcher(router);
        ProtocolHandler protocolHandler = new HttpProtocolHandler(dispatcher);

        MiniWebServer server = new MiniWebServer(
                host,
                port,
                protocolHandler
        );

        server.start();
    }

    /**
     * 注册开发阶段的示例路由。
     */
    private static void registerExampleRoutes(Router router) {
        router.addRoute(
                HttpMethod.GET,
                "/hello",
                (request, pathVariables) -> createResponse(
                        request,
                        "Hello MiniWeb!"
                )
        );

        router.addRoute(
                HttpMethod.GET,
                "/user/{id}",
                (request, pathVariables) -> createResponse(
                        request,
                        "User ID: " + pathVariables.get("id")
                )
        );

        router.addRoute(
                HttpMethod.POST,
                "/user",
                (request, pathVariables) -> createResponse(
                        request,
                        "User created: " + request.getBodyAsString()
                )
        );
    }

    /**
     * 创建简单的文本响应。
     */
    private static HttpResponse createResponse(
            HttpRequest request,
            String message
    ) {
        return new HttpResponse(
                request.getVersion(),
                HttpStatus.OK,
                Map.of("Content-Type", "text/plain; charset=UTF-8"),
                message.getBytes(StandardCharsets.UTF_8)
        );
    }
}