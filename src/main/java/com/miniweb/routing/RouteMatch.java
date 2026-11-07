package com.miniweb.routing;

import com.miniweb.http.HttpMethod;

import java.util.Map;
import java.util.Set;

/**
 * 路由匹配结果。
 *
 * @author zicheng bin
 */
public class RouteMatch {

    /**
     * 路由匹配状态。
     */
    public enum MatchStatus {
        MATCHED,
        NOT_FOUND,
        METHOD_NOT_ALLOWED
    }

    private final MatchStatus status;
    private final RouteDefinition routeDefinition;
    private final Map<String, String> pathVariables;
    private final Set<HttpMethod> allowedMethods;

    private RouteMatch(
            MatchStatus status,
            RouteDefinition routeDefinition,
            Map<String, String> pathVariables,
            Set<HttpMethod> allowedMethods
    ) {
        this.status = status;
        this.routeDefinition = routeDefinition;
        this.pathVariables = Map.copyOf(pathVariables);
        this.allowedMethods = Set.copyOf(allowedMethods);
    }

    /**
     * 创建匹配成功的结果。
     */
    public static RouteMatch matched(
            RouteDefinition routeDefinition,
            Map<String, String> pathVariables
    ) {
        return new RouteMatch(
                MatchStatus.MATCHED,
                routeDefinition,
                pathVariables,
                Set.of()
        );
    }

    /**
     * 创建路径不存在的结果。
     */
    public static RouteMatch notFound() {
        return new RouteMatch(
                MatchStatus.NOT_FOUND,
                null,
                Map.of(),
                Set.of()
        );
    }

    /**
     * 创建请求方法不允许的结果。
     */
    public static RouteMatch methodNotAllowed(Set<HttpMethod> allowedMethods) {
        return new RouteMatch(
                MatchStatus.METHOD_NOT_ALLOWED,
                null,
                Map.of(),
                allowedMethods
        );
    }

    public MatchStatus getStatus() {
        return status;
    }

    public RouteDefinition getRouteDefinition() {
        return routeDefinition;
    }

    public Map<String, String> getPathVariables() {
        return pathVariables;
    }

    public Set<HttpMethod> getAllowedMethods() {
        return allowedMethods;
    }
}