package com.miniweb.routing;

import com.miniweb.http.HttpMethod;

/**
 * 路由定义，描述一条完整的路由规则。
 *
 * @author zicheng bin
 */
public class RouteDefinition {

    private final HttpMethod method;
    private final String path;
    private final RouteHandler handler;

    public RouteDefinition(HttpMethod method, String path, RouteHandler handler) {
        this.method = method;
        this.path = path;
        this.handler = handler;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public RouteHandler getHandler() {
        return handler;
    }
}