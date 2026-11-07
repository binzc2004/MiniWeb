package com.miniweb.routing;

import com.miniweb.http.HttpRequest;
import com.miniweb.http.HttpResponse;

import java.util.Map;

/**
 * 路由处理器接口。
 *
 * @author zicheng bin
 */
@FunctionalInterface
public interface RouteHandler {

    /**
     * 处理匹配到的 HTTP 请求。
     *
     * @param request       HTTP 请求对象
     * @param pathVariables 动态路径参数
     * @return HTTP 响应对象
     */
    HttpResponse handle(
            HttpRequest request,
            Map<String, String> pathVariables);
}