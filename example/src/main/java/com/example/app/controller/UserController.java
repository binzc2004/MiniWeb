package com.example.app.controller;

import com.example.app.model.User;
import com.example.app.repository.InMemoryStore;
import com.miniweb.annotation.GetMapping;
import com.miniweb.annotation.PostMapping;
import com.miniweb.annotation.RestController;
import com.miniweb.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户接口。
 *
 * <p>演示的能力：
 * <ul>
 *     <li>{@code @GetMapping} 静态路径</li>
 *     <li>{@code @GetMapping} 动态路径 {@code /users/{id}}</li>
 *     <li>{@code @PostMapping} + JSON 请求体反序列化为 Java 对象</li>
 *     <li>返回 String 文本响应</li>
 *     <li>返回 Java 对象并由框架序列化为 JSON</li>
 *     <li>通过 {@link HttpRequest} 读取查询参数与请求头</li>
 *     <li>使用 SLF4J 记录业务日志</li>
 * </ul>
 */
@RestController
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final InMemoryStore store = InMemoryStore.getInstance();

    /**
     * 返回纯文本，验证 String 返回值处理。
     */
    @GetMapping("/hello")
    public String hello() {
        log.info("Handling GET /hello");
        return "Hello MiniWeb!";
    }

    /**
     * 返回简单 Java 对象，由框架 JSON 化。
     */
    @GetMapping("/user")
    public User getSingleUser() {
        return new User(1, "Alice", 20, "alice@miniweb.dev");
    }

    /**
     * 返回 List，验证集合类型的 JSON 序列化。
     */
    @GetMapping("/users")
    public List<User> getUsers() {
        List<User> all = store.findAllUsers();
        log.info("Returning {} users", all.size());
        return all;
    }

    /**
     * 动态路径匹配，验证 Router 对 /users/{id} 的匹配能力。
     *
     * <p>根据说明书，路径变量不会自动绑定到方法参数，
     * 因此这里在方法内部通过 {@link HttpRequest} 自行解析路径。
     */
    @GetMapping("/users/{id}")
    public Object getUserById(HttpRequest request) {
        String path = request.getPath();
        String rawId = path.substring(path.lastIndexOf('/') + 1);
        log.info("GET /users/{} -> rawId={}", rawId, rawId);

        Integer id = parseIntOrNull(rawId);
        if (id == null) {
            return "Invalid user id: " + rawId;
        }

        Optional<User> user = store.findUser(id);
        return user.orElseGet(() -> new User(id, "Unknown User", 0, "n/a"));
    }

    /**
     * POST 请求体反序列化为 User 对象并写入内存仓库。
     */
    @PostMapping("/users")
    public User createUser(User user) {
        if (user == null) {
            log.warn("POST /users with empty body");
            return new User(0, "anonymous", 0, "n/a");
        }
        User saved = store.saveUser(user);
        log.info("Created user {}", saved);
        return saved;
    }

    /**
     * 从查询参数读取数据，验证 {@code Map<String, List<String>>} 形式的 Query 参数模型。
     *
     * <p>同时演示同名参数多次出现的场景：{@code /users/search?tag=java&tag=python}。
     */
    @GetMapping("/users/search")
    public Map<String, Object> searchUsers(HttpRequest request) {
        // getQueryParameter 返回 List<String>，参数不存在时返回空列表而不是 null
        String keyword = firstOrEmpty(request.getQueryParameter("keyword"));
        String limitRaw = firstOrEmpty(request.getQueryParameter("limit"));
        List<String> tags = request.getQueryParameter("tag");

        int limit = parseIntOrDefault(limitRaw, 10);
        List<User> matched = new ArrayList<>();
        for (User u : store.findAllUsers()) {
            if (keyword.isBlank() || u.getUsername().toLowerCase().contains(keyword.toLowerCase())) {
                matched.add(u);
            }
            if (matched.size() >= limit) {
                break;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("path", request.getPath());
        result.put("method", request.getMethod().name());
        result.put("keyword", keyword);
        result.put("limit", limit);
        result.put("tags", tags);
        result.put("count", matched.size());
        result.put("items", matched);
        return result;
    }

    /**
     * 回显请求头，验证请求头解析与 {@link HttpRequest} 的公开读取能力。
     *
     * <p>其中 {@code getHeader(name)} 不区分大小写，且请求头不存在时返回 {@code null}。
     */
    @GetMapping("/headers")
    public Map<String, Object> echoHeaders(HttpRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("method", request.getMethod().name());
        result.put("path", request.getPath());
        result.put("version", request.getVersion());
        result.put("headers", request.getHeaders());
        // 大小写不敏感：用小写名去取大写头
        result.put("lowerCaseLookup", request.getHeader("x-demo-token"));
        result.put("missingHeader", request.getHeader("X-Does-Not-Exist"));
        return result;
    }

    /**
     * 故意抛出异常，验证框架对 Controller 异常的处理方式（观察框架默认行为）。
     */
    @GetMapping("/users/boom")
    public String boom() {
        log.warn("About to throw an exception on purpose");
        throw new IllegalStateException("Intentional failure for error-handling demonstration");
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static Integer parseIntOrNull(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        Integer parsed = (value == null || value.isBlank()) ? null : parseIntOrNull(value);
        return parsed == null ? defaultValue : parsed;
    }

    /**
     * 取 {@code List<String>} 的第一个元素，为空时返回空字符串。
     * 对应说明书中 {@code getQueryParameter(name)} 返回空列表（而非 null）的约定。
     */
    private static String firstOrEmpty(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        String first = values.get(0);
        return first == null ? "" : first;
    }
}
