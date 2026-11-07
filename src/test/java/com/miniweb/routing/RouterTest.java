package com.miniweb.routing;

import com.miniweb.http.HttpMethod;
import com.miniweb.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RouterTest {

    private Router router;

    @BeforeEach
    void setUp() {
        router = new Router();
    }

    /**
     * 创建测试用 HTTP 请求。
     */
    private HttpRequest createRequest(HttpMethod method, String path) {
        return new HttpRequest(
                method,
                path,
                "HTTP/1.1",
                Map.of(),
                Map.of(),
                new byte[0]
        );
    }

    /**
     * 创建测试用路由处理器。
     */
    private RouteHandler createHandler() {
        return (request, pathVariables) -> null;
    }

    @Test
    void shouldRegisterRouteSuccessfully() {
        router.addRoute(
                HttpMethod.GET,
                "/hello",
                createHandler()
        );

        RouteMatch result = router.match(
                createRequest(HttpMethod.GET, "/hello")
        );

        assertEquals(RouteMatch.MatchStatus.MATCHED, result.getStatus());
        assertEquals("/hello", result.getRouteDefinition().getPath());
    }

    @Test
    void shouldRejectDuplicateRoute() {
        router.addRoute(HttpMethod.GET, "/user/{id}", createHandler());

        assertThrows(
                IllegalArgumentException.class,
                () -> router.addRoute(
                        HttpMethod.GET,
                        "/user/{name}",
                        createHandler()
                )
        );
    }

    @Test
    void shouldAllowDifferentMethodsForSamePath() {
        router.addRoute(HttpMethod.GET, "/user", createHandler());
        router.addRoute(HttpMethod.POST, "/user", createHandler());

        RouteMatch getResult = router.match(
                createRequest(HttpMethod.GET, "/user")
        );

        RouteMatch postResult = router.match(
                createRequest(HttpMethod.POST, "/user")
        );

        assertEquals(RouteMatch.MatchStatus.MATCHED, getResult.getStatus());
        assertEquals(RouteMatch.MatchStatus.MATCHED, postResult.getStatus());
    }

    @Test
    void shouldMatchExactPath() {
        router.addRoute(HttpMethod.GET, "/hello", createHandler());

        RouteMatch result = router.match(
                createRequest(HttpMethod.GET, "/hello")
        );

        assertEquals(RouteMatch.MatchStatus.MATCHED, result.getStatus());
        assertTrue(result.getPathVariables().isEmpty());
    }

    @Test
    void shouldMatchDynamicPath() {
        router.addRoute(
                HttpMethod.GET,
                "/user/{id}",
                createHandler()
        );

        RouteMatch result = router.match(
                createRequest(HttpMethod.GET, "/user/100")
        );

        assertEquals(RouteMatch.MatchStatus.MATCHED, result.getStatus());
        assertEquals("100", result.getPathVariables().get("id"));
    }

    @Test
    void shouldExtractMultiplePathVariables() {
        router.addRoute(
                HttpMethod.GET,
                "/user/{id}/order/{orderId}",
                createHandler()
        );

        RouteMatch result = router.match(
                createRequest(HttpMethod.GET, "/user/100/order/200")
        );

        assertEquals(RouteMatch.MatchStatus.MATCHED, result.getStatus());
        assertEquals("100", result.getPathVariables().get("id"));
        assertEquals("200", result.getPathVariables().get("orderId"));
    }

    @Test
    void shouldReturnNotFoundWhenPathDoesNotExist() {
        router.addRoute(HttpMethod.GET, "/hello", createHandler());

        RouteMatch result = router.match(
                createRequest(HttpMethod.GET, "/user")
        );

        assertEquals(RouteMatch.MatchStatus.NOT_FOUND, result.getStatus());
    }

    @Test
    void shouldReturnMethodNotAllowed() {
        router.addRoute(HttpMethod.GET, "/user", createHandler());

        RouteMatch result = router.match(
                createRequest(HttpMethod.POST, "/user")
        );

        assertEquals(
                RouteMatch.MatchStatus.METHOD_NOT_ALLOWED,
                result.getStatus()
        );

        assertEquals(
                Set.of(HttpMethod.GET),
                result.getAllowedMethods()
        );
    }

    @Test
    void shouldPrioritizeExactRouteOverDynamicRoute() {
        router.addRoute(
                HttpMethod.GET,
                "/user/{id}",
                createHandler()
        );

        router.addRoute(
                HttpMethod.GET,
                "/user/me",
                createHandler()
        );

        RouteMatch result = router.match(
                createRequest(HttpMethod.GET, "/user/me")
        );

        assertEquals(RouteMatch.MatchStatus.MATCHED, result.getStatus());
        assertEquals("/user/me", result.getRouteDefinition().getPath());
        assertTrue(result.getPathVariables().isEmpty());
    }

    @Test
    void shouldDistinguishTrailingSlash() {
        router.addRoute(
                HttpMethod.GET,
                "/user",
                createHandler()
        );

        RouteMatch result = router.match(
                createRequest(HttpMethod.GET, "/user/")
        );

        assertEquals(RouteMatch.MatchStatus.NOT_FOUND, result.getStatus());
    }

    @Test
    void shouldRejectInvalidDynamicPath() {
        assertThrows(
                IllegalArgumentException.class,
                () -> router.addRoute(
                        HttpMethod.GET,
                        "/user/{id",
                        createHandler()
                )
        );
    }

    @Test
    void shouldRejectDuplicateVariableNames() {
        assertThrows(
                IllegalArgumentException.class,
                () -> router.addRoute(
                        HttpMethod.GET,
                        "/user/{id}/order/{id}",
                        createHandler()
                )
        );
    }
}