package com.example.app.controller;

import com.miniweb.annotation.GetMapping;
import com.miniweb.annotation.RestController;
import com.miniweb.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 框架能力探测接口。
 *
 * <p>这些接口本身没有业务含义，主要用于验证框架层面的行为：
 * <ul>
 *     <li>HTTP/1.0 与 HTTP/1.1 下 {@code HttpRequest#getVersion()} 的取值</li>
 *     <li>Keep-Alive 场景下同一 TCP 连接上的多次请求</li>
 *     <li>纯文本（String）与 JSON（Map / Object）返回值的差异</li>
 *     <li>404 / 405 由框架 Router 产生的响应形态</li>
 * </ul>
 */
@RestController
public class DiagnosticController {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticController.class);

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 纯文本健康检查，返回固定字符串而非 JSON。
     */
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    /**
     * 返回请求元信息的 Map，验证框架对请求各部分的解析。
     *
     * <p>注意各字段的类型：{@code method} 由 {@code HttpMethod#name()} 得到字符串，
     * {@code queryParameters} 为 {@code Map<String, List<String>>}，
     * 请求体以 UTF-8 解码（{@code getBodyAsString()}），同时回显字节长度。
     */
    @GetMapping("/diagnostic/request")
    public Map<String, Object> requestInfo(HttpRequest request) {
        byte[] rawBody = request.getBody();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("method", request.getMethod().name());
        info.put("path", request.getPath());
        info.put("version", request.getVersion());
        info.put("headers", request.getHeaders());
        info.put("queryParameters", request.getQueryParameters());
        info.put("bodyAsString", request.getBodyAsString());
        info.put("bodyLength", rawBody == null ? 0 : rawBody.length);
        info.put("thread", Thread.currentThread().getName());
        info.put("serverTime", LocalDateTime.now().format(FMT));

        log.info("Diagnostic request: {} {} {} on thread {}",
                request.getMethod(), request.getPath(), request.getVersion(),
                Thread.currentThread().getName());
        return info;
    }

    /**
     * 用于验证 Keep-Alive：每次请求返回一个自增序号与时间戳。
     */
    @GetMapping("/diagnostic/keepalive")
    public Map<String, Object> keepAlive(HttpRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("connectionHeader", request.getHeader("Connection"));
        result.put("serverTime", LocalDateTime.now().format(FMT));
        result.put("thread", Thread.currentThread().getName());
        return result;
    }

    /**
     * 用于验证 HTTP/1.0 请求下的响应行为。
     */
    @GetMapping("/diagnostic/http10")
    public String http10(HttpRequest request) {
        return "version=" + request.getVersion();
    }

    /**
     * 返回多行文本，验证响应体的原样输出（不经过 JSON 编码）。
     */
    @GetMapping("/diagnostic/text")
    public String text() {
        return "line1\nline2\nline3";
    }

    /**
     * 回显同名多次出现的查询参数，验证 {@code Map<String, List<String>>} 模型。
     *
     * <p>示例：{@code /diagnostic/query?tag=java&tag=python&page=1}
     */
    @GetMapping("/diagnostic/query")
    public Map<String, Object> query(HttpRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("all", request.getQueryParameters());
        // 同名参数全部返回；不存在的参数返回空列表而非 null
        result.put("tag", request.getQueryParameter("tag"));
        result.put("missing", request.getQueryParameter("does-not-exist"));
        return result;
    }
}
