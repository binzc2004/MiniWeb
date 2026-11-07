# MiniWeb Example

MiniWeb 框架的功能演示项目，尽可能覆盖框架说明书（`how_to_use.md`）中列出的能力。

## 目录结构

```text
example/
├── pom.xml
├── test.py                      # 接口测试脚本
├── README.md
├── static/                      # 静态资源目录（由 StaticResourceHandler 提供）
│   ├── index.html
│   ├── css/style.css
│   ├── js/app.js
│   └── data/sample.json
└── src/main/java/com/example/app/
    ├── Application.java         # @MiniWebApplication 启动类
    ├── controller/
    │   ├── UserController.java       # 用户接口
    │   ├── ProductController.java    # 商品接口
    │   └── DiagnosticController.java # 框架能力探测接口
    ├── model/
    │   ├── User.java
    │   ├── Product.java
    │   └── ApiResponse.java
    └── repository/
        └── InMemoryStore.java   # 内存"数据库"（示例代码，非框架的一部分）
```

## 功能覆盖对照

| 说明书章节 | 覆盖方式 |
|---|---|
| §4 应用配置 | `Application` 上配置 `scanBasePackages` / `host` / `port` / `staticDirectory` |
| §5 Controller | 3 个 `@RestController`（均提供无参构造），共 20 个请求映射方法 |
| §5.4 路由规则 | 精确路径 + 动态路径 + 404 + 405（含 `Allow` 头） |
| §6 HttpRequest | `/diagnostic/request` 回显全部公开 API 的返回值；`/headers` 验证大小写不敏感 |
| §6.5 Query 参数 | `Map<String, List<String>>` 模型；`/diagnostic/query?tag=java&tag=python` 验证同名多值与空列表语义 |
| §6.6 请求体 | `getBody()` 回显字节长度，`getBodyAsString()` 回显 UTF-8 文本 |
| §7 参数解析 | Controller 方法以 `HttpRequest` 作为参数注入 |
| §8 JSON 处理 | String / 对象 / List / Map / 泛型包装 / 嵌套对象 的序列化与反序列化 |
| §9 响应处理 | 文本响应与 JSON 响应两种形态 |
| §10 静态资源 | `index.html`、CSS、JS、JSON 四类静态文件 |
| §11 协议与连接 | HTTP/1.0 与 HTTP/1.1、Keep-Alive、Connection 头 |
| §12 并发 | 测试脚本并发发起 20 个请求，并回显处理线程名 |
| §13 日志 | 各 Controller 使用 SLF4J 记录业务日志 |
| §14 错误处理 | `/users/boom` 主动抛异常，观察框架的 500 处理行为 |

## 运行

前置：需先在本机 Maven 仓库安装 MiniWeb 框架。

```bash
cd <MiniWeb 项目根目录>
mvn clean install
```

启动示例应用：

```bash
cd example
mvn exec:java -Dexec.mainClass="com.example.app.Application"
```

服务默认监听 `http://127.0.0.1:8080`。

## 测试

应用启动后，另开一个终端：

```bash
python test.py
```

可选参数：

```bash
python test.py --base-url http://127.0.0.1:8080   # 指定地址
python test.py -v                                 # 打印响应内容
python test.py --no-color                         # 禁用彩色输出
```

## 接口清单

| Method | Path | 说明 |
|---|---|---|
| GET | `/hello` | 纯文本响应 |
| GET | `/ping` | 健康检查 |
| GET | `/user` | 单个 Java 对象 → JSON |
| GET | `/users` | List 集合 → JSON 数组 |
| GET | `/users/{id}` | 动态路径匹配（路径段在方法内自行解析） |
| GET | `/users/search?keyword=&limit=&tag=` | 查询参数解析（List 语义） |
| POST | `/users` | JSON 请求体 → Java 对象，写入内存 |
| GET | `/headers` | 回显请求头，验证 `getHeader` 大小写不敏感与 null 语义 |
| GET | `/users/boom` | 主动抛异常，观察错误处理 |
| GET | `/products` | 泛型包装对象 → JSON |
| GET | `/products/{id}` | 动态路径 + 包装对象 |
| GET | `/products/{id}/stock` | 多段路径 + Map 响应 |
| POST | `/products` | 嵌套对象反序列化 |
| GET | `/stats` | 统计接口，读取请求头 |
| GET | `/diagnostic/request` | 回显 HttpRequest 全部公开 API 的取值 |
| GET | `/diagnostic/query?tag=a&tag=b` | 验证同名多值查询参数 |
| GET | `/diagnostic/keepalive` | Keep-Alive 验证 |
| GET | `/diagnostic/http10` | HTTP 版本验证 |
| GET | `/diagnostic/text` | 多行文本原样输出 |
| GET | `/index.html`、`/css/style.css`、`/js/app.js`、`/data/sample.json` | 静态资源 |

## 说明

- 示例项目没有真实数据库，`InMemoryStore` 用 `ConcurrentHashMap` 保存数据，进程重启即清空。
- 依据说明书 §5.4，动态路径匹配与路径参数提取是两个不同功能，`HttpRequest` 没有提供路径参数 getter。因此 `/users/{id}` 这类接口在方法内部通过 `getPath()` 自行截取路径段。
- 依据说明书 §9.5、§18.2，Controller **不直接返回 `HttpResponse`**，也不使用 `@PathVariable` / `@RequestParam` / `@RequestBody` / `@RequestHeader`。需要访问请求数据的方法直接声明 `HttpRequest` 参数。
- `HttpRequest` 公开方法清单以说明书 §6.2 / §17.4 为准：`getMethod()`、`getPath()`、`getVersion()`、`getHeaders()`、`getHeader(name)`、`getQueryParameters()`、`getQueryParameter(name)`、`getBody()`、`getBodyAsString()`。本项目未使用清单之外的任何方法。
