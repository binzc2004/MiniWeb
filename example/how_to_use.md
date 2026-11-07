# MiniWeb 使用说明书

**English Documentation:** [How to Use MiniWeb](./how_to_use_en.md)

## 1. 项目简介

MiniWeb 是一个基于 Java 实现的轻量级 Web 框架，旨在通过自主实现 HTTP 服务器、请求解析、路由分发、Controller 管理、参数解析和响应处理等核心模块，理解现代 Web 框架的基本工作原理。

MiniWeb 不依赖 Tomcat、Netty、Spring 等 Web 框架，而是基于 Java Socket 和线程池实现 HTTP 服务，并提供类似 Spring MVC 的注解式开发体验。

使用 MiniWeb 开发 Web 应用时，可以通过 `@MiniWebApplication` 声明应用入口，通过 `@RestController` 声明控制器，通过 `@GetMapping` 和 `@PostMapping` 声明请求映射。

### 1.1 设计目标

- **轻量化：** 不依赖重量级 Web 框架，保持核心实现简单。
- **模块化：** 将服务器、HTTP 协议、路由、控制器和参数处理等功能分层实现。
- **易用性：** 提供注解式 Controller 开发方式。
- **可扩展性：** 核心模块相互解耦，便于扩展协议处理和参数解析能力。
- **可学习性：** 尽可能保留 Web 框架底层实现过程，避免隐藏核心机制。

### 1.2 技术栈

| 技术 | 用途 |
|---|---|
| Java 21 | 开发语言 |
| Maven | 项目构建与依赖管理 |
| Java Socket | 网络通信 |
| Java ExecutorService | 并发请求处理 |
| Jackson | JSON 序列化与反序列化 |
| SLF4J | 日志接口 |
| JUnit 5 | 单元测试 |

---

## 2. 项目架构

MiniWeb 采用分层架构，将 HTTP 请求从网络接入到 Controller 执行，再到响应返回的整个过程拆分为多个独立模块。

### 2.1 核心模块

| 模块 | 主要职责 |
|---|---|
| `server` | 创建服务器、监听端口、接收客户端连接、管理线程池 |
| `protocol` | 定义协议处理接口 |
| `http` | HTTP 请求解析、请求和响应对象、HTTP 协议处理 |
| `routing` | 路由注册、路径匹配、请求分发 |
| `controller` | Controller 扫描、注册、参数解析和返回值处理 |
| `annotation` | 定义应用启动和请求映射相关注解 |
| `static` | 静态资源处理 |

### 2.2 HTTP 请求处理流程

一次完整的 HTTP 请求大致经历以下过程：

1. `MiniWebServer` 启动服务器，监听指定端口。
2. 客户端建立 Socket 连接。
3. `ClientHandler` 接收连接，并将处理任务提交给线程池。
4. `HttpProtocolHandler` 读取 HTTP 请求数据。
5. `HttpParser` 将原始 HTTP 数据解析为 `HttpRequest`。
6. `Dispatcher` 接收请求并进行分发。
7. `Router` 根据请求路径和 HTTP Method 查找对应的路由。
8. 如果匹配到 Controller 路由，则执行对应的 `HandlerMethod`。
9. `ArgumentResolver` 负责解析 Controller 方法参数。
10. Controller 执行业务逻辑。
11. `ReturnValueHandler` 处理 Controller 返回值，生成 HTTP 响应。
12. `HttpProtocolHandler` 将 `HttpResponse` 写回客户端。
13. 如果连接采用 Keep-Alive，则继续处理同一连接上的后续请求。

如果没有匹配到 Controller 路由，Dispatcher 会尝试通过 `StaticResourceHandler` 处理静态资源；如果请求方法不匹配，则返回 405；如果路径和静态资源均无法处理，则返回 404。

### 2.3 核心调用关系

```text
MiniWeb
  ├── ControllerScanner
  ├── ControllerRegistry
  ├── Router
  ├── ArgumentResolver
  ├── ReturnValueHandler
  ├── StaticResourceHandler
  ├── Dispatcher
  │     ├── Router
  │     └── StaticResourceHandler
  ├── HttpProtocolHandler
  │     └── Dispatcher
  └── MiniWebServer
        └── ClientHandler
              └── ProtocolHandler
```

---

## 3. 环境要求与安装

### 3.1 环境要求

- JDK 21 或更高版本。
- Maven 3.8 或更高版本。
- 支持 Java Socket 的操作系统。

### 3.2 Maven 依赖

MiniWeb 当前坐标：

- Group ID：`io.github.binzc2004`
- Artifact ID：`miniweb`
- Version：`1.0.0-SNAPSHOT`

在 Maven 项目中添加：

```xml
<dependency>
    <groupId>io.github.binzc2004</groupId>
    <artifactId>miniweb</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

目前该版本需要先安装到本地 Maven 仓库，或者发布到 Maven 仓库后才能供其他项目正常依赖。

在 MiniWeb 源码项目根目录执行：

```powershell
mvn clean install
```

如果仅希望安装而暂时跳过测试：

```powershell
mvn clean install -DskipTests
```

注意：`-DskipTests` 会跳过测试执行，不能据此判断测试是否通过。

---

## 4. 创建应用

### 4.1 应用入口

MiniWeb 使用 `@MiniWebApplication` 标记应用入口类。

```java
package com.example;

import com.miniweb.MiniWeb;
import com.miniweb.annotation.MiniWebApplication;

@MiniWebApplication
public class Application {
    public static void main(String[] args) {
        MiniWeb.run(Application.class);
    }
}
```

调用 `MiniWeb.run()` 后，框架会读取应用配置，扫描 Controller，完成路由注册，并创建 HTTP 服务器。

### 4.2 应用配置

`@MiniWebApplication` 支持以下配置：

| 属性 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `scanBasePackages` | String | `""` | Controller 扫描包 |
| `host` | String | `"127.0.0.1"` | 服务器监听地址 |
| `port` | int | `8080` | 服务器监听端口 |
| `staticDirectory` | String | `"static"` | 静态资源目录 |

例如：

```java
@MiniWebApplication(
    scanBasePackages = "com.example.controller",
    host = "0.0.0.0",
    port = 8081,
    staticDirectory = "static"
)
public class Application {
    public static void main(String[] args) {
        MiniWeb.run(Application.class);
    }
}
```

如果 `scanBasePackages` 为空，框架默认扫描应用入口类所在的包。

---

## 5. Controller 开发

### 5.1 声明 Controller

使用 `@RestController` 声明一个控制器：

```java
package com.example.controller;

import com.miniweb.annotation.RestController;

@RestController
public class HelloController {
}
```

Controller 由框架扫描并实例化。目前 Controller 需要提供无参构造方法。

### 5.2 GET 请求映射

使用 `@GetMapping` 声明 GET 请求对应的处理方法：

```java
package com.example.controller;

import com.miniweb.annotation.GetMapping;
import com.miniweb.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello MiniWeb!";
    }
}
```

请求：

```http
GET /hello HTTP/1.1
Host: localhost:8080
```

### 5.3 POST 请求映射

使用 `@PostMapping` 声明 POST 请求对应的处理方法：

```java
package com.example.controller;

import com.miniweb.annotation.PostMapping;
import com.miniweb.annotation.RestController;

@RestController
public class UserController {
    @PostMapping("/users")
    public String createUser() {
        return "User created!";
    }
}
```

请求：

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Length: 0
```

### 5.4 路由规则

MiniWeb 的 Router 支持：

- 精确路径匹配。
- 动态路径匹配。
- 根据 HTTP Method 区分路由。
- 路径不存在时返回 404。
- 路径存在但请求方法不匹配时返回 405。
- 405 响应包含允许使用的 HTTP Method 信息（`Allow`）。

需要注意：动态路径匹配与路径参数提取是两个不同的功能。当前已确认的 `HttpRequest` API 没有提供专门的路径参数 getter，也不能直接假设框架支持 Spring 的 `@PathVariable`。如果需要在 Controller 中获取动态路径中的变量，应以实际 `RouteMatch`、`HandlerMethod` 和参数解析实现为准。

---

## 6. HttpRequest 请求对象

`HttpRequest` 是 MiniWeb 对 HTTP 请求的封装。HTTP Parser 将原始网络数据解析后，生成该对象，供 Dispatcher、参数解析器和 Controller 使用。

Package：

```java
com.miniweb.http.HttpRequest
```

### 6.1 构造方法

```java
public HttpRequest(
    HttpMethod method,
    String path,
    String version,
    Map<String, String> headers,
    Map<String, List<String>> queryParameters,
    byte[] body
)
```

字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| `method` | `HttpMethod` | HTTP 请求方法 |
| `path` | `String` | 请求路径，不包含 Query 参数 |
| `version` | `String` | HTTP 协议版本 |
| `headers` | `Map<String, String>` | 请求头 |
| `queryParameters` | `Map<String, List<String>>` | 查询参数 |
| `body` | `byte[]` | 请求体原始字节 |

### 6.2 完整公开方法

```java
public HttpMethod getMethod();

public String getPath();

public String getVersion();

public Map<String, String> getHeaders();

public String getHeader(String name);

public Map<String, List<String>> getQueryParameters();

public List<String> getQueryParameter(String name);

public byte[] getBody();

public String getBodyAsString();
```

### 6.3 请求方法与路径

```java
HttpMethod method = request.getMethod();
String path = request.getPath();
String version = request.getVersion();
```

`getPath()` 返回不包含 Query 参数的路径。

例如：

```text
/users?page=1
```

调用 `getPath()` 得到：

```text
/users
```

### 6.4 请求头

请求头使用 `Map<String, String>` 保存。

获取全部请求头：

```java
Map<String, String> headers = request.getHeaders();
```

获取指定请求头：

```java
String authorization = request.getHeader("Authorization");
String contentType = request.getHeader("Content-Type");
```

`getHeader(name)` 不区分请求头名称大小写。如果请求头不存在，返回 `null`。

### 6.5 Query 参数

Query 参数使用：

```java
Map<String, List<String>>
```

保存，因此支持同名参数出现多次的情况。

获取全部 Query 参数：

```java
Map<String, List<String>> parameters = request.getQueryParameters();
```

获取指定参数的全部值：

```java
List<String> tags = request.getQueryParameter("tag");
```

例如：

```http
GET /search?tag=java&tag=python&page=1 HTTP/1.1
```

对应结果：

```java
request.getQueryParameter("tag");
// ["java", "python"]

request.getQueryParameter("page");
// ["1"]
```

如果参数不存在，`getQueryParameter(name)` 返回空列表，而不是 `null`。

注意：`getQueryParameters()` 的值类型是 `List<String>`，不能将它当作 `Map<String, String>` 使用。

### 6.6 请求体

获取原始请求体：

```java
byte[] body = request.getBody();
```

获取 UTF-8 字符串：

```java
String body = request.getBodyAsString();
```

例如，客户端发送 JSON：

```json
{
    "username": "bubu",
    "age": 18
}
```

可以通过：

```java
String json = request.getBodyAsString();
```

获取 JSON 字符串。

如果需要将 JSON 转换成 Java 对象，应使用框架提供的 JSON 参数解析能力，具体支持的 Controller 参数声明形式以实际 `ArgumentResolver` 实现为准。

### 6.7 对象封装特性

`HttpRequest` 在构造时会复制请求头和 Query 参数 Map；请求体在构造时复制，在调用 `getBody()` 时也会返回副本。

因此，调用者不能通过修改返回的请求体字节数组来直接修改内部请求数据。

---

## 7. Controller 参数解析

MiniWeb 通过 `ArgumentResolver` 负责将 `HttpRequest` 中的数据转换为 Controller 方法所需的参数。

相关模块：

- `ArgumentResolver`
- `TypeConverter`
- `JsonConverter`

整体职责：

| 组件 | 职责 |
|---|---|
| `ArgumentResolver` | 根据 Controller 方法参数信息解析参数 |
| `TypeConverter` | 处理基础类型转换 |
| `JsonConverter` | 处理 JSON 数据转换 |

Controller 不应该直接承担底层 HTTP 字节解析工作。请求数据应由 HTTP 层封装，再交由参数解析模块进行转换。

### 7.1 参数解析边界

当前已经确认 `HttpRequest` 提供以下数据获取方式：

```java
request.getHeader("Authorization");

request.getQueryParameter("page");

request.getQueryParameters();

request.getBody();

request.getBodyAsString();
```

以上接口可用于理解参数来源。

对于 Controller 方法究竟支持哪些参数类型、如何声明请求对象参数、JSON 参数如何绑定，以及是否支持路径参数注入，应以 `ArgumentResolver` 的实际实现为准，不应直接照搬 Spring MVC 的注解或参数规则。

---

## 8. JSON 处理

MiniWeb 使用 Jackson Databind 处理 JSON 序列化与反序列化。

相关组件：

```text
JsonConverter
ArgumentResolver
ReturnValueHandler
```

### 8.1 JSON 请求

客户端可以发送 JSON 格式的请求体：

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 36

{"username":"bubu","age":18}
```

HTTP 层负责读取请求体，`HttpRequest` 保存请求体字节，后续由参数解析模块负责数据转换。

### 8.2 JSON 响应

Controller 返回值由 `ReturnValueHandler` 处理。框架使用 JSON 转换组件完成相应的序列化工作。

具体哪些 Java 返回类型会被识别为 JSON 响应，以及返回字符串时的处理规则，应以当前 `ReturnValueHandler` 的实现为准。

---

## 9. HttpResponse 响应对象

`HttpResponse` 是 MiniWeb 对 HTTP 响应的封装，负责保存协议版本、状态码、响应头和响应体。

Package：

```java
com.miniweb.http.HttpResponse
```

### 9.1 构造方法

```java
public HttpResponse(
    String version,
    HttpStatus status,
    Map<String, String> headers,
    byte[] body
)
```

字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| `version` | `String` | HTTP 协议版本 |
| `status` | `HttpStatus` | HTTP 状态码 |
| `headers` | `Map<String, String>` | 响应头 |
| `body` | `byte[]` | 响应体原始字节 |

### 9.2 完整公开方法

```java
public String getVersion();

public HttpStatus getStatus();

public Map<String, String> getHeaders();

public String getHeader(String name);

public byte[] getBody();

public String getBodyAsString();
```

### 9.3 创建响应对象

例如：

```java
import com.miniweb.http.HttpResponse;
import com.miniweb.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

Map<String, String> headers = new HashMap<>();
headers.put("Content-Type", "text/plain; charset=UTF-8");

HttpResponse response = new HttpResponse(
    "HTTP/1.1",
    HttpStatus.OK,
    headers,
    "Hello MiniWeb!".getBytes(StandardCharsets.UTF_8)
);
```

### 9.4 获取响应数据

```java
String version = response.getVersion();

HttpStatus status = response.getStatus();

Map<String, String> headers = response.getHeaders();

String contentType = response.getHeader("Content-Type");

byte[] body = response.getBody();

String text = response.getBodyAsString();
```

`getHeader(name)` 不区分响应头名称大小写，不存在时返回 `null`。

`getBodyAsString()` 使用 UTF-8 解码。

### 9.5 封装特性

`HttpResponse` 在构造时会复制响应头 Map，响应体也会进行防御性复制。

因此，调用者无法通过修改原始 Map 或 `getBody()` 返回的数组来修改内部数据。

注意：上述构造方法说明的是 `HttpResponse` 对象的公开 API，并不代表 Controller 可以直接返回 `HttpResponse`。Controller 返回值的实际支持范围应以 `ReturnValueHandler` 实现为准。

---

## 10. 静态资源处理

MiniWeb 提供静态资源处理能力。

通过 `@MiniWebApplication` 的 `staticDirectory` 属性指定静态资源目录：

```java
@MiniWebApplication(
    staticDirectory = "static"
)
public class Application {
    public static void main(String[] args) {
        MiniWeb.run(Application.class);
    }
}
```

默认目录为：

```text
static/
```

静态资源处理由 `StaticResourceHandler` 负责，并由 `Dispatcher` 在没有匹配到 Controller 路由时进行处理。

例如：

```text
example/
├── src/
└── static/
    ├── index.html
    ├── css/
    │   └── style.css
    └── js/
        └── app.js
```

静态资源的具体 MIME 类型识别、目录访问限制和资源路径安全规则，应以 `StaticResourceHandler` 当前实现为准。

---

## 11. HTTP 协议支持

MiniWeb 通过 `HttpProtocolHandler` 实现 HTTP 协议处理。

当前实现涉及以下能力：

| 功能 | 说明 |
|---|---|
| HTTP/1.0 | 支持 HTTP/1.0 请求 |
| HTTP/1.1 | 支持 HTTP/1.1 请求 |
| Content-Length | 根据请求头读取请求体 |
| CRLF | 按 HTTP 协议要求处理行结束符 |
| Keep-Alive | 支持持久连接 |
| Socket Timeout | 设置客户端 Socket 读取超时时间 |
| 请求体限制 | 最大请求体大小为 10 MB |

### 11.1 HTTP 协议处理接口

MiniWeb 定义了 `ProtocolHandler` 接口，由具体协议处理器负责处理客户端 Socket。

HTTP 实现：

```java
com.miniweb.http.HttpProtocolHandler
```

其构造方法：

```java
public HttpProtocolHandler(Dispatcher dispatcher)
```

### 11.2 Keep-Alive

HTTP/1.1 默认采用持久连接语义，MiniWeb 会根据连接相关请求头处理连接生命周期。

在持久连接下，同一个 Socket 可以依次处理多个 HTTP 请求，而不是每次请求都重新建立连接。

### 11.3 请求体大小限制

当前请求体最大限制为 10 MB。

超过限制的请求不应被当作普通请求体继续无限读取，具体错误响应和连接处理方式以 HTTP 解析器实现为准。

---

## 12. 并发处理

MiniWeb 使用 Java 线程池处理客户端连接，避免每个请求都无限制地创建新线程。

当前服务器配置：

| 配置 | 值 |
|---|---|
| 核心线程数 | 10 |
| 最大线程数 | 10 |
| 阻塞队列容量 | 100 |
| 拒绝策略 | `AbortPolicy` |
| Socket 读取超时 | 30 秒 |

服务器接收到客户端连接后，将连接处理任务提交给线程池。

当线程池和任务队列均达到容量限制时，拒绝策略会触发，服务器关闭无法提交处理任务的 Socket。

### 12.1 并发模型

```text
Client 1 ── Socket ── ClientHandler ── Thread 1
Client 2 ── Socket ── ClientHandler ── Thread 2
Client 3 ── Socket ── ClientHandler ── Thread 3
                         ...
Client N ── Socket ── ClientHandler ── Thread N
```

该模型可以在一定程度上控制并发连接数量，避免线程无限增长。

需要注意，线程池处理的是客户端连接任务，Keep-Alive 连接上的多个请求可以在同一个客户端处理任务中依次执行。

---

## 13. 日志

MiniWeb 使用 SLF4J 作为日志接口。

框架开发者可以通过 SLF4J 记录：

- 服务器启动和关闭。
- 客户端连接建立和关闭。
- HTTP 请求解析。
- 路由匹配。
- Controller 执行。
- 请求处理异常。
- 线程池拒绝任务。

建议将日志级别合理划分：

| 级别 | 使用场景 |
|---|---|
| DEBUG | 请求处理流程、路由匹配、Socket 生命周期等调试信息 |
| INFO | 服务器启动、关闭等重要事件 |
| WARN | 可恢复异常、非预期但不影响整体运行的情况 |
| ERROR | 请求处理失败、服务器关键错误 |

MiniWeb 仅依赖 SLF4J API，不应将某一种日志实现强制绑定给所有使用者。实际应用可以根据需要配置 Logback 等日志实现。

---

## 14. 错误处理

MiniWeb 当前涉及以下常见 HTTP 错误：

| 状态码 | 含义 | 典型场景 |
|---|---|---|
| 400 | Bad Request | HTTP 请求格式错误 |
| 404 | Not Found | 路由和静态资源均无法匹配 |
| 405 | Method Not Allowed | 路径存在，但 HTTP Method 不匹配 |
| 500 | Internal Server Error | 请求处理过程中发生服务器内部错误 |

### 14.1 404

当 Router 无法找到对应路径，并且静态资源处理器也无法处理请求时，Dispatcher 返回 404。

### 14.2 405

当路径存在，但请求方法与已注册路由不匹配时，返回 405，并提供 `Allow` 响应头。

### 14.3 500

Controller 或请求处理过程发生未能正常处理的异常时，应由框架的异常处理流程转换为适当的错误响应。

具体异常捕获层级、错误响应格式和异常信息是否对外暴露，应以实际实现为准。生产环境不应直接向客户端暴露内部堆栈信息。

---

## 15. 完整示例

下面给出一个简单的应用结构：

```text
example/
├── pom.xml
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   ├── Application.java
│                   └── controller/
│                       ├── HelloController.java
│                       └── UserController.java
└── static/
    └── index.html
```

### 15.1 Application.java

```java
package com.example;

import com.miniweb.MiniWeb;
import com.miniweb.annotation.MiniWebApplication;

@MiniWebApplication(
    host = "127.0.0.1",
    port = 8080,
    staticDirectory = "static"
)
public class Application {
    public static void main(String[] args) {
        MiniWeb.run(Application.class);
    }
}
```

### 15.2 HelloController.java

```java
package com.example.controller;

import com.miniweb.annotation.GetMapping;
import com.miniweb.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello MiniWeb!";
    }

    @GetMapping("/hello/json")
    public Object helloJson() {
        return java.util.Map.of(
            "message", "Hello MiniWeb!",
            "version", "1.0.0"
        );
    }
}
```

注意：`Object` 返回值是否按 JSON 序列化，应以当前 `ReturnValueHandler` 实现为准。

### 15.3 UserController.java

下面示例展示一个使用内存数据模拟用户管理的业务结构。Controller 参数声明和返回值类型需要根据框架当前 `ArgumentResolver`、`ReturnValueHandler` 的实际支持范围调整。

```java
package com.example.controller;

import com.miniweb.annotation.GetMapping;
import com.miniweb.annotation.PostMapping;
import com.miniweb.annotation.RestController;
import com.miniweb.http.HttpRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class UserController {
    private final List<Map<String, Object>> users = new CopyOnWriteArrayList<>();

    public UserController() {
        users.add(Map.of("id", 1, "username", "bubu"));
        users.add(Map.of("id", 2, "username", "yier"));
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        return new ArrayList<>(users);
    }

    @GetMapping("/users/search")
    public Map<String, Object> searchUsers(HttpRequest request) {
        String keyword = request.getQueryParameter("keyword")
            .stream()
            .findFirst()
            .orElse("");

        List<Map<String, Object>> result = users.stream()
            .filter(user -> user.get("username").toString().contains(keyword))
            .toList();

        return Map.of(
            "keyword", keyword,
            "users", result
        );
    }

    @GetMapping("/request/info")
    public Map<String, Object> requestInfo(HttpRequest request) {
        return Map.of(
            "method", request.getMethod().name(),
            "path", request.getPath(),
            "version", request.getVersion(),
            "userAgent", String.valueOf(request.getHeader("User-Agent")),
            "queryParameters", request.getQueryParameters()
        );
    }
}
```

**重要：** 以上示例中的 `HttpRequest` 作为 Controller 方法参数是否能够由当前 `ArgumentResolver` 自动注入，以及集合、Map 等返回值如何处理，需要以实际源码为准。示例用于展示业务组织方式，不应将未经确认的参数绑定能力视为框架已支持功能。

---

## 16. 启动与测试

### 16.1 启动应用

在应用项目根目录执行：

```powershell
mvn clean package
```

如果项目配置了 Maven Exec 插件，可以执行：

```powershell
mvn exec:java "-Dexec.mainClass=com.example.Application"
```

也可以通过 IDE 直接运行 `Application.main()`。

### 16.2 测试 HTTP 接口

使用 curl：

```powershell
curl http://127.0.0.1:8080/hello
```

测试 JSON 接口：

```powershell
curl http://127.0.0.1:8080/hello/json
```

测试 Query 参数：

```powershell
curl "http://127.0.0.1:8080/users/search?keyword=bu"
```

测试请求头：

```powershell
curl -H "User-Agent: MiniWeb-Test" http://127.0.0.1:8080/request/info
```

POST 请求示例：

```powershell
curl -X POST http://127.0.0.1:8080/users `
    -H "Content-Type: application/json" `
    -d '{"username":"new-user","age":18}'
```

POST 示例能否直接映射 JSON 对象到 Controller 参数，需要依据 `ArgumentResolver` 的具体实现。

---

## 17. API 参考

### 17.1 应用启动

```java
MiniWeb.run(Application.class);
```

### 17.2 应用注解

```java
@MiniWebApplication
```

支持配置：

```java
scanBasePackages
host
port
staticDirectory
```

### 17.3 Controller 注解

```java
@RestController
@GetMapping("/path")
@PostMapping("/path")
```

### 17.4 HTTP 请求对象

```java
HttpRequest(
    HttpMethod method,
    String path,
    String version,
    Map<String, String> headers,
    Map<String, List<String>> queryParameters,
    byte[] body
)
```

公开方法：

```java
getMethod()
getPath()
getVersion()
getHeaders()
getHeader(String name)
getQueryParameters()
getQueryParameter(String name)
getBody()
getBodyAsString()
```

### 17.5 HTTP 响应对象

```java
HttpResponse(
    String version,
    HttpStatus status,
    Map<String, String> headers,
    byte[] body
)
```

公开方法：

```java
getVersion()
getStatus()
getHeaders()
getHeader(String name)
getBody()
getBodyAsString()
```

### 17.6 协议处理

```java
new HttpProtocolHandler(dispatcher);
```

### 17.7 请求分发

```java
new Dispatcher(router, staticResourceHandler);
```

---

## 18. 当前功能边界与后续扩展

MiniWeb 当前重点在于实现一个轻量级、可运行、具备基本 Web 开发体验的 HTTP 框架，而不是完整复刻 Spring Boot。

### 18.1 当前已实现的核心能力

- Java Socket HTTP 服务器。
- 固定大小线程池处理客户端连接。
- HTTP/1.0 和 HTTP/1.1 基本处理。
- Keep-Alive。
- HTTP 请求解析和请求对象封装。
- HTTP 响应对象封装。
- GET、POST 请求映射。
- 精确路径和动态路径匹配。
- 404、405 等基本路由错误处理。
- Controller 注解扫描和注册。
- Controller 方法参数解析基础设施。
- Jackson JSON 转换基础设施。
- 静态资源处理。
- SLF4J 日志接口。

### 18.2 不应默认认为已支持的能力

以下能力需要以具体源码为准，不能因为 Spring MVC 支持就认为 MiniWeb 也支持：

- `@PathVariable` 路径参数注入。
- `@RequestParam` 参数注入。
- `@RequestBody` 注解绑定。
- `@RequestHeader` 注解绑定。
- Controller 直接返回 `HttpResponse`。
- 全局异常处理注解。
- 依赖注入和自动装配。
- 拦截器和过滤器。
- 文件上传。
- WebSocket。
- HTTPS。
- Cookie 与 Session。
- 身份认证与权限控制。
- 数据库集成。
- 复杂的参数校验。

### 18.3 后续可扩展方向

在当前框架基础上，可以继续考虑：

1. 完善动态路径参数提取与传递。
2. 增加更明确的请求参数绑定规则。
3. 完善统一异常处理机制。
4. 增加请求拦截器。
5. 增加依赖注入容器。
6. 增加文件上传和下载。
7. 增加 HTTPS 支持。
8. 完善服务器生命周期管理。
9. 增加更完整的集成测试。
10. 发布稳定版本至 Maven Central。

---

## 19. 总结

MiniWeb 从底层 Socket 通信开始，逐步实现 HTTP 协议解析、路由匹配、请求分发、Controller 管理、参数解析、响应处理和静态资源访问等核心功能。

它的核心价值不仅在于提供一个轻量级 Web 开发框架，更在于通过模块化设计还原现代 Web 框架的基本工作机制。

对于开发者而言，可以通过 MiniWeb 了解一次 HTTP 请求从进入服务器到最终返回响应的完整过程，也可以在此基础上继续扩展自己的 Web 框架。