package com.miniweb.routing;

import com.miniweb.http.HttpMethod;
import com.miniweb.http.HttpRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 路由管理与匹配器。
 *
 * @author zicheng bin
 */
public class Router {

    /**
     * 已注册的路由集合。
     */
    private final List<RouteDefinition> routes = new ArrayList<>();

    /**
     * 注册一条路由。
     *
     * @param method  HTTP 请求方法
     * @param path    路由路径
     * @param handler 请求处理器
     */
    public void addRoute(HttpMethod method, String path, RouteHandler handler) {
        validateRoute(method, path, handler);

        for (RouteDefinition route : routes) {
            if (route.getMethod() == method
                    && isSameRoutePattern(route.getPath(), path)) {
                throw new IllegalArgumentException(
                        "Duplicate route: " + method + " " + path);
            }
        }

        routes.add(new RouteDefinition(method, path, handler));
    }

    /**
     * 根据 HTTP 请求查找对应路由。
     *
     * @param request HTTP 请求
     * @return 路由匹配结果
     */
    public RouteMatch match(HttpRequest request) {
        String path = request.getPath();
        HttpMethod method = request.getMethod();

        // 第一轮：优先查找精确路径
        Set<HttpMethod> exactMethods = new LinkedHashSet<>();

        for (RouteDefinition route : routes) {
            if (route.getPath().equals(path)) {
                exactMethods.add(route.getMethod());

                if (route.getMethod() == method) {
                    return RouteMatch.matched(route, Map.of());
                }
            }
        }

        if (!exactMethods.isEmpty()) {
            return RouteMatch.methodNotAllowed(exactMethods);
        }

        // 第二轮：查找动态路径
        Set<HttpMethod> allowedMethods = new LinkedHashSet<>();

        for (RouteDefinition route : routes) {
            Map<String, String> variables = matchPath(route.getPath(), path);

            if (variables == null) {
                continue;
            }

            allowedMethods.add(route.getMethod());

            if (route.getMethod() == method) {
                return RouteMatch.matched(route, variables);
            }
        }

        if (!allowedMethods.isEmpty()) {
            return RouteMatch.methodNotAllowed(allowedMethods);
        }

        return RouteMatch.notFound();
    }

    /**
     * 匹配动态路径并提取路径参数。
     *
     * @param routePath   注册的路由路径
     * @param requestPath 实际请求路径
     * @return 匹配成功返回参数集合，否则返回 null
     */
    private Map<String, String> matchPath(
            String routePath,
            String requestPath) {
        String[] routeSegments = splitPath(routePath);
        String[] requestSegments = splitPath(requestPath);

        if (routeSegments.length != requestSegments.length) {
            return null;
        }

        Map<String, String> variables = new LinkedHashMap<>();

        for (int i = 0; i < routeSegments.length; i++) {
            String routeSegment = routeSegments[i];
            String requestSegment = requestSegments[i];

            if (isDynamicSegment(routeSegment)) {
                String variableName = routeSegment.substring(
                        1, routeSegment.length() - 1);

                variables.put(variableName, requestSegment);
            } else if (!routeSegment.equals(requestSegment)) {
                return null;
            }
        }

        return variables;
    }

    /**
     * 判断两条路由是否具有相同的匹配结构。
     *
     * 例如：
     * /user/{id} 和 /user/{name} 视为相同结构。
     */
    private boolean isSameRoutePattern(String path1, String path2) {
        String[] segments1 = splitPath(path1);
        String[] segments2 = splitPath(path2);

        if (segments1.length != segments2.length) {
            return false;
        }

        for (int i = 0; i < segments1.length; i++) {
            boolean dynamic1 = isDynamicSegment(segments1[i]);
            boolean dynamic2 = isDynamicSegment(segments2[i]);

            if (dynamic1 && dynamic2) {
                continue;
            }

            if (!segments1[i].equals(segments2[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * 判断一个路径段是否为动态参数。
     */
    private boolean isDynamicSegment(String segment) {
        return segment.startsWith("{")
                && segment.endsWith("}")
                && segment.length() > 2;
    }

    /**
     * 拆分路径。
     */
    private String[] splitPath(String path) {
        if ("/".equals(path)) {
            return new String[0];
        }

        return path.substring(1).split("/", -1);
    }

    /**
     * 校验路由定义。
     */
    private void validateRoute(
            HttpMethod method,
            String path,
            RouteHandler handler) {
        if (method == null) {
            throw new IllegalArgumentException("HTTP method cannot be null");
        }

        if (path == null || path.isBlank() || !path.startsWith("/")) {
            throw new IllegalArgumentException(
                    "Route path must start with '/': " + path);
        }

        if (handler == null) {
            throw new IllegalArgumentException("Route handler cannot be null");
        }

        String[] segments = splitPath(path);
        Set<String> variableNames = new LinkedHashSet<>();

        for (String segment : segments) {
            if (segment.contains("{") || segment.contains("}")) {
                if (!isDynamicSegment(segment)) {
                    throw new IllegalArgumentException(
                            "Invalid dynamic path segment: " + segment);
                }

                String variableName = segment.substring(
                        1, segment.length() - 1);

                if (!variableNames.add(variableName)) {
                    throw new IllegalArgumentException(
                            "Duplicate path variable: " + variableName);
                }
            }
        }
    }
}