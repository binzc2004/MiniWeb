
package com.miniweb.controller;

import com.miniweb.annotation.PathVariable;
import com.miniweb.annotation.RequestBody;
import com.miniweb.annotation.RequestParam;
import com.miniweb.http.HttpMethod;
import com.miniweb.http.HttpRequest;
import com.miniweb.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Controller 组件测试。
 *
 * @author zicheng bin
 */
class ControllerComponentTest {

    private final TypeConverter typeConverter = new TypeConverter();
    private final JsonConverter jsonConverter = new JsonConverter();

    private final ArgumentResolver argumentResolver =
            new ArgumentResolver(typeConverter, jsonConverter);

    private final ReturnValueHandler returnValueHandler =
            new ReturnValueHandler(jsonConverter);

    static class User {
        public int id;
        public String name;

        public User() {
        }

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class TestController {

        public void getUser(
                @PathVariable("id") int id,
                @RequestParam("name") String name
        ) {
        }

        public void createUser(@RequestBody User user) {
        }
    }

    /**
     * 测试基础类型转换。
     */
    @Test
    void shouldConvertBasicTypes() {
        assertEquals(100, typeConverter.convert("100", int.class));
        assertEquals(100L, typeConverter.convert("100", long.class));
        assertEquals(3.14, typeConverter.convert("3.14", double.class));
        assertEquals(true, typeConverter.convert("true", boolean.class));
        assertEquals('A', typeConverter.convert("A", char.class));
        assertEquals("Tom", typeConverter.convert("Tom", String.class));
    }

    /**
     * 测试不合法的类型转换。
     */
    @Test
    void shouldRejectInvalidTypeConversion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> typeConverter.convert("abc", int.class)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> typeConverter.convert("yes", boolean.class)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> typeConverter.convert("AB", char.class)
        );
    }

    /**
     * 测试 JSON 序列化和反序列化。
     */
    @Test
    void shouldConvertJson() {
        User user = new User(100, "Tom");

        String json = jsonConverter.toJson(user);

        assertTrue(json.contains("\"id\":100"));
        assertTrue(json.contains("\"name\":\"Tom\""));

        User result = jsonConverter.fromJson(json, User.class);

        assertEquals(100, result.id);
        assertEquals("Tom", result.name);
    }

    /**
     * 测试 PathVariable 和 RequestParam 参数解析。
     */
    @Test
    void shouldResolvePathAndQueryParameters() throws Exception {
        TestController controller = new TestController();

        var method = TestController.class.getMethod(
                "getUser", int.class, String.class
        );

        HttpRequest request = new HttpRequest(
                HttpMethod.GET,
                "/user/100",
                "HTTP/1.1",
                Map.of(),
                Map.of("name", List.of("Tom")),
                new byte[0]
        );

        Object[] arguments = argumentResolver.resolveArguments(
                method,
                request,
                Map.of("id", "100")
        );

        assertEquals(100, arguments[0]);
        assertEquals("Tom", arguments[1]);
    }

    /**
     * 测试 RequestBody 参数解析。
     */
    @Test
    void shouldResolveRequestBody() throws Exception {
        var method = TestController.class.getMethod(
                "createUser", User.class
        );

        HttpRequest request = new HttpRequest(
                HttpMethod.POST,
                "/user",
                "HTTP/1.1",
                Map.of(),
                Map.of(),
                "{\"id\":100,\"name\":\"Tom\"}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        Object[] arguments = argumentResolver.resolveArguments(
                method,
                request,
                Map.of()
        );

        User user = (User) arguments[0];

        assertEquals(100, user.id);
        assertEquals("Tom", user.name);
    }

    /**
     * 测试缺失参数。
     */
    @Test
    void shouldRejectMissingQueryParameter() throws Exception {
        var method = TestController.class.getMethod(
                "getUser", int.class, String.class
        );

        HttpRequest request = new HttpRequest(
                HttpMethod.GET,
                "/user/100",
                "HTTP/1.1",
                Map.of(),
                Map.of(),
                new byte[0]
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> argumentResolver.resolveArguments(
                        method,
                        request,
                        Map.of("id", "100")
                )
        );
    }

    /**
     * 测试返回值处理。
     */
    @Test
    void shouldHandleStringReturnValue() {
        var response = returnValueHandler.handle("Hello MiniWeb!");

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals("Hello MiniWeb!", response.getBodyAsString());
        assertEquals(
                "text/plain; charset=UTF-8",
                response.getHeader("Content-Type")
        );
    }

    /**
     * 测试对象返回值处理。
     */
    @Test
    void shouldHandleObjectReturnValue() {
        User user = new User(100, "Tom");

        var response = returnValueHandler.handle(user);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getBodyAsString().contains("\"id\":100"));
        assertEquals(
                "application/json; charset=UTF-8",
                response.getHeader("Content-Type")
        );
    }

    /**
     * 测试 null 返回值。
     */
    @Test
    void shouldHandleNullReturnValue() {
        var response = returnValueHandler.handle(null);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatus());
        assertEquals(0, response.getBody().length);
    }
}
