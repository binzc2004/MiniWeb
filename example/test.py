#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MiniWeb 示例项目测试脚本。

在 example 目录下先启动应用：

    mvn exec:java -Dexec.mainClass="com.example.app.Application"

再运行本脚本：

    python test.py
    python test.py --base-url http://127.0.0.1:8080
    python test.py -v          # 打印每个用例的响应体

脚本覆盖框架说明书中的以下能力：
    - @RestController / @GetMapping / @PostMapping 路由注册
    - 静态路径与动态路径匹配
    - 404 Not Found / 405 Method Not Allowed（含 Allow 响应头）
    - String 文本返回值与 Java 对象 JSON 序列化
    - 请求参数（Query Parameter）解析，Map<String, List<String>> 模型
    - HttpRequest 全部公开 API（getMethod / getPath / getVersion / getHeaders /
      getHeader / getQueryParameters / getQueryParameter / getBody / getBodyAsString）
    - 请求头大小写不敏感解析
    - JSON 请求体反序列化
    - 静态资源处理
    - Keep-Alive 长连接
    - HTTP/1.0 与 HTTP/1.1
    - 并发请求处理
"""

import argparse
import http.client
import json
import socket
import sys
import threading
import time
import urllib.error
import urllib.request

# ----------------------------------------------------------------------
# 测试框架（极简实现，避免额外依赖）
# ----------------------------------------------------------------------

PASSED = []
FAILED = []
VERBOSE = False


class Colors:
    GREEN = "\033[92m"
    RED = "\033[91m"
    YELLOW = "\033[93m"
    CYAN = "\033[96m"
    GREY = "\033[90m"
    RESET = "\033[0m"

    @classmethod
    def disable(cls):
        cls.GREEN = cls.RED = cls.YELLOW = cls.CYAN = cls.GREY = cls.RESET = ""


def info(message):
    print("%s%s%s" % (Colors.CYAN, message, Colors.RESET))


def check(name, condition, detail=""):
    if condition:
        PASSED.append(name)
        print("  %s[PASS]%s %s" % (Colors.GREEN, Colors.RESET, name))
    else:
        FAILED.append((name, detail))
        print("  %s[FAIL]%s %s" % (Colors.RED, Colors.RESET, name))
        if detail:
            print("         %s%s%s" % (Colors.GREY, detail, Colors.RESET))
    return condition


def verbose_dump(label, value):
    if VERBOSE:
        text = value if isinstance(value, str) else str(value)
        if len(text) > 800:
            text = text[:800] + " …(truncated)"
        print("         %s%s: %s%s" % (Colors.GREY, label, text, Colors.RESET))


# ----------------------------------------------------------------------
# HTTP 工具
# ----------------------------------------------------------------------

BASE_URL = "http://127.0.0.1:8080"


def request(method, path, body=None, headers=None):
    """
    发送 HTTP 请求，返回 (status, headers, body_text)。

    使用 urllib 并在 HTTPError 时依然读取响应体，便于检查 404/405 的响应内容。
    """
    url = BASE_URL + path
    data = None
    hdrs = dict(headers or {})
    if body is not None:
        data = body.encode("utf-8") if isinstance(body, str) else body
        hdrs.setdefault("Content-Type", "application/json")

    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return resp.status, dict(resp.headers), resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, dict(e.headers), e.read().decode("utf-8", "replace")


def get(path, headers=None):
    return request("GET", path, None, headers)


def post(path, body, headers=None):
    return request("POST", path, body, headers)


def parse_json(text):
    try:
        return json.loads(text)
    except (ValueError, TypeError):
        return None


def wait_for_server(timeout=40.0):
    """等待服务端端口可连接，避免脚本先于应用启动时误报失败。"""
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            with socket.create_connection(("127.0.0.1", PORT), timeout=1):
                return True
        except OSError:
            time.sleep(0.5)
    return False


# ----------------------------------------------------------------------
# 测试分组
# ----------------------------------------------------------------------

def test_basic_routes():
    info("\n[1] 基础路由与返回值处理（@GetMapping / String / Java 对象）")

    status, _, body = get("/hello")
    check("GET /hello 返回 200", status == 200, "status=%s" % status)
    check("GET /hello 返回纯文本 'Hello MiniWeb!'", body == "Hello MiniWeb!", "body=%r" % body)
    verbose_dump("body", body)

    status, headers, _ = get("/hello")
    ctype = headers.get("Content-Type", "")
    check("文本响应带 Content-Type", ctype != "", "Content-Type=%r" % ctype)
    verbose_dump("Content-Type", ctype)

    status, _, body = get("/ping")
    check("GET /ping 返回 200 且为 'pong'", status == 200 and body == "pong",
          "status=%s body=%r" % (status, body))

    # Java 对象 -> JSON
    status, headers, body = get("/user")
    check("GET /user 返回 200", status == 200, "status=%s" % status)
    obj = parse_json(body)
    check("GET /user 返回合法 JSON", obj is not None, "body=%r" % body)
    if obj:
        check("JSON 包含 id=1", obj.get("id") == 1, "obj=%s" % obj)
        check("JSON 包含 username='Alice'", obj.get("username") == "Alice", "obj=%s" % obj)
        check("JSON 包含 age=20", obj.get("age") == 20, "obj=%s" % obj)
    verbose_dump("body", body)

    status, _, body = get("/diagnostic/text")
    check("多行文本原样返回（未被 JSON 编码）", status == 200 and body == "line1\nline2\nline3",
          "body=%r" % body)


def test_collection_and_generics():
    info("\n[2] 集合与泛型对象的 JSON 序列化")

    status, _, body = get("/users")
    check("GET /users 返回 200", status == 200, "status=%s" % status)
    users = parse_json(body)
    check("GET /users 返回 JSON 数组", isinstance(users, list), "body=%r" % body[:200])
    if isinstance(users, list):
        check("预置假数据至少 3 条", len(users) >= 3, "len=%d" % len(users))
        check("数组元素为对象且含 username",
              all(isinstance(u, dict) and "username" in u for u in users),
              "first=%s" % (users[0] if users else None))
    verbose_dump("body", body)

    status, _, body = get("/products")
    check("GET /products 返回 200", status == 200, "status=%s" % status)
    wrapper = parse_json(body)
    check("泛型包装对象含 code/message/data 字段",
          isinstance(wrapper, dict)
          and "code" in wrapper and "message" in wrapper and "data" in wrapper,
          "wrapper=%s" % (str(wrapper)[:200],))
    if isinstance(wrapper, dict) and isinstance(wrapper.get("data"), list):
        first = wrapper["data"][0] if wrapper["data"] else {}
        check("嵌套对象 Category 被序列化",
              isinstance(first.get("category"), dict) and "name" in first["category"],
              "first=%s" % first)
        check("boolean 字段序列化为 true/false",
              isinstance(first.get("available"), bool), "first=%s" % first)
        check("double 字段存在", "price" in first, "first=%s" % first)
    verbose_dump("body", body)


def test_dynamic_routes():
    info("\n[3] 动态路径匹配（/users/{id}、/products/{id}）")

    status, _, body = get("/users/1")
    check("GET /users/1 返回 200（动态路径匹配成功）", status == 200, "status=%s" % status)
    obj = parse_json(body)
    check("动态路径返回 JSON 对象", isinstance(obj, dict), "body=%r" % body[:200])
    if isinstance(obj, dict):
        check("通过 HttpRequest 取到的 id 正确", obj.get("id") == 1, "obj=%s" % obj)
    verbose_dump("body", body)

    status, _, body = get("/users/2")
    obj = parse_json(body)
    check("GET /users/2 命中同一路由且 id=2",
          status == 200 and isinstance(obj, dict) and obj.get("id") == 2,
          "status=%s body=%r" % (status, body[:200]))

    # 动态路径匹配非数字段也应命中路由（框架不区分类型）
    status, _, body = get("/users/alice")
    check("GET /users/alice 路由命中（路径段不限定类型）", status == 200,
          "status=%s body=%r" % (status, body[:200]))
    verbose_dump("body", body)

    status, _, body = get("/products/1")
    wrapper = parse_json(body)
    check("GET /products/1 返回包装对象",
          status == 200 and isinstance(wrapper, dict) and wrapper.get("code") == 200,
          "status=%s body=%r" % (status, body[:200]))

    status, _, body = get("/products/9999")
    wrapper = parse_json(body)
    check("不存在的商品返回业务级 not found（HTTP 仍为 200）",
          status == 200 and isinstance(wrapper, dict) and wrapper.get("code") == 500,
          "status=%s body=%r" % (status, body[:200]))
    verbose_dump("body", body)

    status, _, body = get("/products/1/stock")
    check("GET /products/1/stock 多段静态路径 + 动态段 返回 200", status == 200,
          "status=%s body=%r" % (status, body[:200]))
    verbose_dump("body", body)


def test_query_parameters():
    info("\n[4] 查询参数解析（Query Parameters，Map<String, List<String>>）")

    status, _, body = get("/users/search?keyword=al&limit=2")
    check("GET /users/search?keyword=al&limit=2 返回 200", status == 200, "status=%s" % status)
    obj = parse_json(body)
    check("返回结构包含 keyword 字段", isinstance(obj, dict) and "keyword" in obj,
          "body=%r" % body[:200])
    if isinstance(obj, dict):
        check("keyword 被正确解析为 'al'", obj.get("keyword") == "al", "obj=%s" % obj)
        check("limit 被解析为整数 2", obj.get("limit") == 2, "obj=%s" % obj)
        check("结果条数不超过 limit", obj.get("count", 99) <= 2, "obj=%s" % obj)
        check("未传的 tag 参数返回空列表而非 null",
              obj.get("tags") == [], "tags=%r" % obj.get("tags"))
    verbose_dump("body", body)

    status, _, body = get("/users/search")
    obj = parse_json(body)
    check("无查询参数时默认 limit=10",
          status == 200 and isinstance(obj, dict) and obj.get("limit") == 10,
          "status=%s body=%r" % (status, body[:200]))

    # 同名参数出现多次：Map<String, List<String>> 的核心特征
    status, _, body = get("/diagnostic/query?tag=java&tag=python&page=1")
    check("GET /diagnostic/query 返回 200", status == 200, "status=%s" % status)
    obj = parse_json(body)
    check("同名参数 tag 被收集为列表",
          isinstance(obj, dict) and obj.get("tag") == ["java", "python"],
          "tag=%r" % (obj or {}).get("tag"))
    if isinstance(obj, dict):
        allparams = obj.get("all") or {}
        check("getQueryParameters() 的值为 List<String> 类型",
              isinstance(allparams, dict)
              and isinstance(allparams.get("page"), list)
              and allparams["page"] == ["1"],
              "all=%s" % allparams)
        check("getQueryParameter 对不存在的参数返回空列表",
              obj.get("missing") == [], "missing=%r" % obj.get("missing"))
    verbose_dump("body", body)

    status, _, body = get("/diagnostic/request?a=1&b=two&flag=true")
    obj = parse_json(body)
    check("诊断接口回显 queryParameters",
          isinstance(obj, dict) and isinstance(obj.get("queryParameters"), dict),
          "body=%r" % body[:300])
    if isinstance(obj, dict):
        params = obj.get("queryParameters") or {}
        check("query 参数 a 为列表 ['1']", params.get("a") == ["1"], "params=%s" % params)
        check("query 参数 b 为列表 ['two']", params.get("b") == ["two"], "params=%s" % params)
    verbose_dump("body", body)


def test_headers():
    info("\n[5] 请求头解析（Request Headers，getHeader 大小写不敏感）")

    status, _, body = get("/headers", headers={"X-Demo-Token": "miniweb-test", "Accept": "application/json"})
    check("GET /headers 返回 200", status == 200, "status=%s" % status)
    obj = parse_json(body)
    check("响应包含 headers 字段", isinstance(obj, dict) and isinstance(obj.get("headers"), dict),
          "body=%r" % body[:300])
    if isinstance(obj, dict):
        hdrs = obj.get("headers") or {}
        found = any(k.lower() == "x-demo-token" for k in hdrs)
        check("自定义请求头 X-Demo-Token 被框架解析", found, "headers=%s" % list(hdrs))
        check("getHeader() 大小写不敏感（用小写名取大写头）",
              obj.get("lowerCaseLookup") == "miniweb-test",
              "lowerCaseLookup=%r" % obj.get("lowerCaseLookup"))
        check("getHeader() 对不存在的请求头返回 null",
              obj.get("missingHeader") is None,
              "missingHeader=%r" % obj.get("missingHeader"))
        check("getMethod().name() 返回 'GET'", obj.get("method") == "GET",
              "method=%r" % obj.get("method"))
    verbose_dump("body", body)

    status, _, body = get("/stats", headers={"User-Agent": "MiniWebTest/1.0"})
    obj = parse_json(body)
    check("业务接口可读取 User-Agent 请求头",
          isinstance(obj, dict) and (obj.get("data") or {}).get("clientAgent") == "MiniWebTest/1.0",
          "body=%r" % body[:300])


def test_http_request_api():
    info("\n[5b] HttpRequest 公开 API 覆盖")

    status, _, body = get("/diagnostic/request?x=1")
    obj = parse_json(body)
    check("GET /diagnostic/request 返回 200", status == 200, "status=%s" % status)
    if isinstance(obj, dict):
        check("getMethod() 经 name() 得到字符串 'GET'", obj.get("method") == "GET",
              "method=%r" % obj.get("method"))
        check("getPath() 不含 query 参数", obj.get("path") == "/diagnostic/request",
              "path=%r" % obj.get("path"))
        check("getVersion() 返回协议版本字符串",
              isinstance(obj.get("version"), str) and "HTTP" in str(obj.get("version")),
              "version=%r" % obj.get("version"))
        check("getHeaders() 返回 Map", isinstance(obj.get("headers"), dict),
              "headers=%r" % str(obj.get("headers"))[:120])
        check("getQueryParameters() 返回嵌套 Map", isinstance(obj.get("queryParameters"), dict),
              "queryParameters=%r" % obj.get("queryParameters"))
        check("getBodyAsString() 对空请求体返回空串",
              obj.get("bodyAsString") == "", "bodyAsString=%r" % obj.get("bodyAsString"))
        check("getBody() 返回 byte[]，空请求体长度为 0",
              obj.get("bodyLength") == 0, "bodyLength=%r" % obj.get("bodyLength"))

    # 带请求体时 getBody() / getBodyAsString()
    payload = '{"username":"BodyCheck","age":22}'
    status, _, body = post("/diagnostic/request", payload)
    obj = parse_json(body)
    check("带请求体时 getBodyAsString() 返回原始 JSON",
          isinstance(obj, dict) and obj.get("bodyAsString") == payload,
          "bodyAsString=%r" % (obj or {}).get("bodyAsString"))
    check("带请求体时 getBody() 长度与内容长度一致",
          isinstance(obj, dict) and obj.get("bodyLength") == len(payload.encode("utf-8")),
          "bodyLength=%r expected=%d" % ((obj or {}).get("bodyLength"), len(payload.encode("utf-8"))))
    verbose_dump("body", body)


def test_post_and_json_body():
    info("\n[6] POST 请求与 JSON 请求体反序列化")

    payload = {"username": "Dave", "age": 28, "email": "dave@miniweb.dev"}
    status, _, body = post("/users", json.dumps(payload))
    check("POST /users 返回 200", status == 200, "status=%s" % status)
    obj = parse_json(body)
    check("请求体被反序列化为 Java 对象",
          isinstance(obj, dict) and obj.get("username") == "Dave",
          "body=%r" % body[:200])
    if isinstance(obj, dict):
        check("框架为新建对象分配了 id", isinstance(obj.get("id"), int) and obj["id"] > 0,
              "obj=%s" % obj)
        check("age 字段类型转换正确", obj.get("age") == 28, "obj=%s" % obj)
        new_id = obj.get("id")
    else:
        new_id = None
    verbose_dump("body", body)

    # 写入后应能从内存仓库读到
    if new_id:
        status, _, body = get("/users")
        users = parse_json(body) or []
        check("POST 写入的数据可被后续 GET 读取",
              any(u.get("id") == new_id and u.get("username") == "Dave" for u in users),
              "new_id=%s" % new_id)

        status, _, body = get("/users/%d" % new_id)
        obj = parse_json(body)
        check("按新 id 调用动态路径可命中该记录",
              isinstance(obj, dict) and obj.get("username") == "Dave",
              "status=%s body=%r" % (status, body[:200]))

    status, _, body = post("/products", json.dumps({
        "name": "Trackball Mouse",
        "price": 329.5,
        "stock": 7,
        "available": True,
        "category": {"id": 1, "name": "Peripherals"}
    }))
    wrapper = parse_json(body)
    check("POST /products 嵌套对象反序列化成功",
          isinstance(wrapper, dict)
          and (wrapper.get("data") or {}).get("category", {}).get("name") == "Peripherals",
          "body=%r" % body[:300])
    verbose_dump("body", body)

    status, _, body = post("/users", "")
    check("空请求体不会导致服务端 500", status < 500, "status=%s body=%r" % (status, body[:200]))


def test_error_handling():
    info("\n[7] 路由错误处理（404 / 405）")

    status, _, body = get("/this/path/does/not/exist")
    check("未知路径返回 404 Not Found", status == 404, "status=%s body=%r" % (status, body[:200]))
    verbose_dump("body", body)

    status, headers, body = get("/not-found")
    check("GET /not-found 返回 404", status == 404, "status=%s" % status)

    # 405：路径存在但方法不匹配
    status, headers, body = request("DELETE", "/hello")
    check("DELETE /hello 返回 405 Method Not Allowed", status == 405,
          "status=%s body=%r" % (status, body[:200]))
    allow = None
    for k, v in headers.items():
        if k.lower() == "allow":
            allow = v
    check("405 响应包含 Allow 响应头", allow is not None, "headers=%s" % list(headers))
    if allow:
        check("Allow 头包含 GET", "GET" in allow.upper(), "Allow=%r" % allow)
    verbose_dump("Allow", allow)
    verbose_dump("body", body)

    status, _, body = request("DELETE", "/users")
    check("DELETE /users（仅注册了 GET/POST）返回 405", status == 405,
          "status=%s body=%r" % (status, body[:200]))

    # /users/search 是静态注册路径，DELETE 应 405
    status, _, body = request("PUT", "/ping")
    check("PUT /ping 返回 405", status == 405, "status=%s" % status)

    # 404 与 405 的区分
    status_404, _, _ = get("/definitely-missing-endpoint")
    status_405, _, _ = request("DELETE", "/ping")
    check("框架能够区分 404 与 405",
          status_404 == 404 and status_405 == 405,
          "404=%s 405=%s" % (status_404, status_405))


def test_static_resources():
    info("\n[8] 静态资源处理（StaticResourceHandler）")

    status, headers, body = get("/index.html")
    check("GET /index.html 返回 200", status == 200, "status=%s" % status)
    check("index.html 内容正确", "<!DOCTYPE html>" in body and "MiniWeb" in body,
          "body=%r" % body[:120])
    check("静态资源带 Content-Type", headers.get("Content-Type", "") != "",
          "Content-Type=%r" % headers.get("Content-Type"))
    verbose_dump("Content-Type", headers.get("Content-Type"))

    status, headers, body = get("/css/style.css")
    check("GET /css/style.css 返回 200 且含 CSS 内容",
          status == 200 and ":root" in body, "status=%s body=%r" % (status, body[:120]))
    verbose_dump("Content-Type", headers.get("Content-Type"))

    status, _, body = get("/js/app.js")
    check("GET /js/app.js 返回 200 且含 JS 内容",
          status == 200 and "fetch" in body, "status=%s body=%r" % (status, body[:120]))

    status, _, body = get("/data/sample.json")
    payload = parse_json(body)
    check("GET /data/sample.json 返回可解析的 JSON",
          status == 200 and isinstance(payload, dict) and payload.get("application") == "MiniWeb Example",
          "status=%s body=%r" % (status, body[:200]))
    verbose_dump("body", body)

    status, _, body = get("/missing-asset.css")
    check("不存在的静态资源返回 404", status == 404, "status=%s body=%r" % (status, body[:200]))

    # 静态资源与 Controller 路由互不干扰
    status1, _, body1 = get("/hello")
    status2, _, _ = get("/css/style.css")
    check("Controller 路由与静态资源处理器可共存",
          status1 == 200 and body1 == "Hello MiniWeb!" and status2 == 200,
          "hello=%s css=%s" % (status1, status2))


def test_http_versions():
    info("\n[9] HTTP 协议版本处理（HTTP/1.0 与 HTTP/1.1）")

    # HTTP/1.1
    conn = http.client.HTTPConnection("127.0.0.1", PORT, timeout=10)
    try:
        conn.request("GET", "/diagnostic/request", headers={"Host": "127.0.0.1:%d" % PORT})
        resp = conn.getresponse()
        raw = resp.read().decode("utf-8", "replace")
        obj = parse_json(raw)
        check("HTTP/1.1 请求处理成功", resp.status == 200, "status=%s" % resp.status)
        check("HttpRequest 记录了 HTTP 版本",
              isinstance(obj, dict) and "1.1" in str(obj.get("version")),
              "version=%s" % (obj or {}).get("version"))
    finally:
        conn.close()

    # HTTP/1.0
    conn = http.client.HTTPConnection("127.0.0.1", PORT, timeout=10)
    try:
        conn._http_vsn = 10
        conn._http_vsn_str = "HTTP/1.0"
        conn.request("GET", "/diagnostic/request")
        resp = conn.getresponse()
        raw = resp.read().decode("utf-8", "replace")
        obj = parse_json(raw)
        check("HTTP/1.0 请求处理成功", resp.status == 200, "status=%s" % resp.status)
        if isinstance(obj, dict):
            check("HTTP/1.0 下 version 字段被正确记录",
                  "1.0" in str(obj.get("version")), "version=%s" % obj.get("version"))
        verbose_dump("version", (obj or {}).get("version"))
    finally:
        conn.close()


def test_keep_alive():
    info("\n[10] Keep-Alive 长连接复用")

    conn = http.client.HTTPConnection("127.0.0.1", PORT, timeout=10)
    reuse_ok = True
    try:
        for i in range(3):
            conn.request("GET", "/diagnostic/keepalive",
                         headers={"Host": "127.0.0.1:%d" % PORT})
            resp = conn.getresponse()
            raw = resp.read().decode("utf-8", "replace")
            if resp.status != 200:
                reuse_ok = False
                break
            obj = parse_json(raw)
            if not isinstance(obj, dict) or obj.get("ok") is not True:
                reuse_ok = False
                break
            if i == 0:
                verbose_dump("first response", raw)
        check("同一 TCP 连接上连续发送 3 个请求均成功（Keep-Alive 生效）", reuse_ok)
    except Exception as e:  # noqa: BLE001
        check("同一 TCP 连接上连续发送 3 个请求均成功（Keep-Alive 生效）", False, str(e))
    finally:
        conn.close()

    # 服务端返回的 Connection 头
    conn = http.client.HTTPConnection("127.0.0.1", PORT, timeout=10)
    try:
        conn.request("GET", "/hello", headers={"Host": "127.0.0.1:%d" % PORT,
                                               "Connection": "keep-alive"})
        resp = conn.getresponse()
        resp.read()
        connhdr = resp.getheader("Connection")
        check("HTTP/1.1 请求 Connection 头存在或被省略（默认为长连接）",
              connhdr is None or "keep-alive" in connhdr.lower(),
              "Connection=%r" % connhdr)
        verbose_dump("Connection", connhdr)
        check("响应包含 Content-Length 或分帧信息",
              resp.getheader("Content-Length") is not None or resp.getheader("Transfer-Encoding") is not None,
              "Content-Length=%r" % resp.getheader("Content-Length"))
    finally:
        conn.close()


def test_concurrency():
    info("\n[11] 并发处理（固定大小线程池）")

    results = []
    lock = threading.Lock()

    def worker(index):
        try:
            status, _, body = get("/diagnostic/request?worker=%d" % index)
            with lock:
                results.append((index, status, body))
        except Exception as e:  # noqa: BLE001
            with lock:
                results.append((index, -1, str(e)))

    threads = [threading.Thread(target=worker, args=(i,)) for i in range(20)]
    start = time.time()
    for t in threads:
        t.start()
    for t in threads:
        t.join(timeout=30)
    elapsed = time.time() - start

    ok = [r for r in results if r[1] == 200]
    check("20 个并发请求全部成功", len(ok) == 20,
          "ok=%d/%d, failures=%s" % (len(ok), len(results),
                                     [r for r in results if r[1] != 200][:3]))
    print("         %s耗时 %.2fs%s" % (Colors.GREY, elapsed, Colors.RESET))

    threads_seen = set()
    for _, _, body in ok:
        obj = parse_json(body)
        if isinstance(obj, dict) and obj.get("thread"):
            threads_seen.add(obj["thread"])
    check("请求由线程池中的工作线程处理", len(threads_seen) > 0,
          "threads=%s" % threads_seen)
    print("         %s工作线程: %s%s" % (Colors.GREY, sorted(threads_seen), Colors.RESET))


def test_persistence_within_process():
    info("\n[12] 内存数据一致性（模拟数据库读写链路）")

    before_status, _, before_body = get("/stats")
    before = parse_json(before_body) or {}
    before_count = (before.get("data") or {}).get("productCount")

    create_status, _, create_body = post("/products", json.dumps({
        "name": "Laptop Stand",
        "price": 129.0,
        "stock": 20,
        "available": True,
        "category": {"id": 2, "name": "Accessories"}
    }))
    created = parse_json(create_body) or {}
    created_id = (created.get("data") or {}).get("id")

    after_status, _, after_body = get("/stats")
    after = parse_json(after_body) or {}
    after_count = (after.get("data") or {}).get("productCount")

    check("POST 创建商品成功", create_status == 200 and created_id is not None,
          "status=%s body=%r" % (create_status, create_body[:200]))
    check("统计接口反映新增数据",
          isinstance(before_count, int) and isinstance(after_count, int)
          and after_count == before_count + 1,
          "before=%s after=%s" % (before_count, after_count))

    status, _, body = get("/products")
    wrapper = parse_json(body) or {}
    items = wrapper.get("data") or []
    check("新增商品出现在列表中",
          any(p.get("id") == created_id for p in items), "created_id=%s" % created_id)

    status, _, body = get("/products/%s/stock" % created_id)
    stock = parse_json(body) or {}
    check("查询新增商品库存返回 200 与正确数据",
          status == 200 and stock.get("stock") == 20, "status=%s body=%r" % (status, body[:200]))
    verbose_dump("body", body)


def test_json_serialization_details():
    info("\n[13] JSON 转换细节（序列化与反序列化）")

    status, headers, body = get("/user")
    ctype = headers.get("Content-Type", "").lower()
    check("JSON 响应 Content-Type 为 application/json",
          "application/json" in ctype, "Content-Type=%r" % ctype)

    status, headers, body = get("/hello")
    check("文本响应 Content-Type 不是 application/json",
          "application/json" not in headers.get("Content-Type", "").lower(),
          "Content-Type=%r" % headers.get("Content-Type"))

    obj = parse_json(get("/user")[2])
    check("对象字段名为 Java 属性名（驼峰）",
          isinstance(obj, dict) and "username" in obj, "obj=%s" % obj)

    # 反序列化：字段顺序无关、含未知字段
    status, _, body = post("/users", json.dumps({
        "unknownField": "ignored",
        "email": "order@miniweb.dev",
        "age": 33,
        "username": "OrderTest"
    }))
    obj = parse_json(body)
    check("JSON 字段顺序无关，均可正确绑定",
          isinstance(obj, dict) and obj.get("username") == "OrderTest"
          and obj.get("age") == 33 and obj.get("email") == "order@miniweb.dev",
          "body=%r" % body[:200])
    verbose_dump("body", body)

    # 类型转换：数字以字符串形式提交
    status, _, body = post("/users", json.dumps({"username": "Coerce", "age": 40}))
    obj = parse_json(body)
    check("整数字段被正确序列化为 JSON number",
          isinstance(obj, dict) and isinstance(obj.get("age"), int),
          "body=%r" % body[:200])


# ----------------------------------------------------------------------
# main
# ----------------------------------------------------------------------

def main():
    global BASE_URL, PORT, VERBOSE

    parser = argparse.ArgumentParser(description="MiniWeb 示例项目测试脚本")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080",
                        help="应用基地址，默认 http://127.0.0.1:8080")
    parser.add_argument("--no-color", action="store_true", help="禁用彩色输出")
    parser.add_argument("-v", "--verbose", action="store_true", help="打印响应内容")
    parser.add_argument("--no-wait", action="store_true", help="不等待服务端启动")
    args = parser.parse_args()

    BASE_URL = args.base_url.rstrip("/")
    PORT = 8080
    try:
        PORT = int(BASE_URL.rsplit(":", 1)[1].split("/")[0])
    except (IndexError, ValueError):
        pass
    VERBOSE = args.verbose

    if args.no_color:
        Colors.disable()

    print("=" * 68)
    print(" MiniWeb Example 测试脚本")
    print(" 目标地址: %s" % BASE_URL)
    print("=" * 68)

    if not args.no_wait:
        info("\n等待服务端就绪…")
        if not wait_for_server():
            print("%s\n服务端在 %s 上不可达。请先启动应用：%s\n"
                  "    mvn exec:java -Dexec.mainClass=\"com.example.app.Application\"\n%s"
                  % (Colors.RED, BASE_URL, Colors.GREY, Colors.RESET))
            return 2
        info("服务端已就绪。")

    groups = [
        test_basic_routes,
        test_collection_and_generics,
        test_dynamic_routes,
        test_query_parameters,
        test_headers,
        test_http_request_api,
        test_post_and_json_body,
        test_error_handling,
        test_static_resources,
        test_http_versions,
        test_keep_alive,
        test_concurrency,
        test_persistence_within_process,
        test_json_serialization_details,
    ]

    for group in groups:
        try:
            group()
        except Exception as e:  # noqa: BLE001
            FAILED.append((group.__name__, "unexpected error: %r" % e))
            print("  %s[ERROR]%s %s 抛出异常: %r"
                  % (Colors.RED, Colors.RESET, group.__name__, e))

    print("\n" + "=" * 68)
    total = len(PASSED) + len(FAILED)
    print(" 通过: %s%d%s    失败: %s%d%s    共 %d 项"
          % (Colors.GREEN, len(PASSED), Colors.RESET,
             Colors.RED if FAILED else Colors.GREEN, len(FAILED), Colors.RESET,
             total))
    if FAILED:
        print("\n 失败用例:")
        for name, detail in FAILED:
            print("   - %s" % name)
            if detail:
                print("       %s" % detail)
    else:
        print("\n %s全部通过%s" % (Colors.GREEN, Colors.RESET))
    print("=" * 68)

    return 1 if FAILED else 0


if __name__ == "__main__":
    sys.exit(main())
