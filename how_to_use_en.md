# MiniWeb — How to Use

**MiniWeb** is a lightweight Java Web framework built on top of Java Socket. It provides HTTP server, request parsing, routing, controller registration, parameter resolution, JSON conversion, static resource handling, and application auto-configuration.

MiniWeb aims to simplify Java Web application development while keeping its underlying implementation lightweight and understandable.

This document introduces the framework's architecture, public APIs, configuration, and practical usage.

---

# 1. Overview

## 1.1 Features

| Component | Description |
|---|---|
| HTTP Server | TCP server implemented using Java Socket |
| HTTP Protocol | HTTP/1.0 and HTTP/1.1 |
| Connection Management | Keep-Alive and socket timeout |
| Concurrency | Fixed-size thread pool |
| HTTP Parser | Request line, headers, and body parsing |
| HttpRequest | Encapsulation of HTTP requests |
| HttpResponse | Encapsulation of HTTP responses |
| Router | HTTP method and URL-based routing |
| Dynamic Routing | Static and dynamic path matching |
| Controller | Annotation-based controller registration |
| Argument Resolver | Controller method argument resolution |
| Type Converter | Request parameter type conversion |
| JSON Converter | JSON serialization and deserialization |
| Return Value Handler | Controller return value processing |
| Static Resources | Static file serving |
| Logging | SLF4J-based logging |
| Auto Configuration | Annotation-based application startup |

## 1.2 Request Processing Lifecycle

MiniWeb processes an HTTP request through the following components:

```text
                   Client
                      |
                      v
                MiniWebServer
                      |
                      v
                 ClientHandler
                      |
                      v
              HttpProtocolHandler
                      |
                      v
                  HttpParser
                      |
                      v
                 HttpRequest
                      |
                      v
                  Dispatcher
                      |
              +-------+-------+
              |               |
              v               v
            Router      StaticResourceHandler
              |
              v
         RouteHandler
              |
              v
         HandlerMethod
              |
              v
          Controller
              |
              v
       ReturnValueHandler
              |
              v
          HttpResponse
              |
              v
             Client
```

The framework encapsulates the underlying TCP and HTTP processing. Application developers mainly work with application configuration, controllers, and business logic.

---

# 2. Requirements and Installation

## 2.1 Requirements

- Java 21 or later
- Maven 3.8 or later

## 2.2 Maven Dependency

Add MiniWeb to your application's `pom.xml`:

```xml
<dependency>
    <groupId>com.miniweb</groupId>
    <artifactId>miniweb</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

The coordinates above represent the current development configuration. They must match the actual coordinates defined in MiniWeb's `pom.xml`.

During local development, install MiniWeb into the local Maven repository:

```bash
mvn clean install
```

Maven will package the framework and install its artifact into the local repository, usually located at:

```text
~/.m2/repository/
```

On Windows:

```text
%USERPROFILE%\.m2\repository\
```

After installation, another Maven project can resolve MiniWeb using the dependency declaration above.

---

# 3. Application Configuration

## 3.1 @MiniWebApplication

`@MiniWebApplication` is the application entry-point annotation.

It declares the basic configuration required to start a MiniWeb application.

Example:

```java
package com.example.app;

import com.miniweb.MiniWeb;
import com.miniweb.annotation.MiniWebApplication;

@MiniWebApplication
public class Application {

    public static void main(String[] args) {
        MiniWeb.run(Application.class);
    }
}
```

The application is started by calling:

```java
MiniWeb.run(Application.class);
```

MiniWeb reads the application annotation and assembles the required framework components.

## 3.2 Configuration Properties

`@MiniWebApplication` currently provides the following properties:

| Property | Default | Description |
|---|---|---|
| `scanBasePackages` | `""` | Base package used for controller scanning |
| `host` | `"127.0.0.1"` | Server listening address |
| `port` | `8080` | Server listening port |
| `staticDirectory` | `"static"` | Static resource directory |

Example:

```java
@MiniWebApplication(
    scanBasePackages = "com.example.app",
    host = "0.0.0.0",
    port = 8080,
    staticDirectory = "static"
)
public class Application {

    public static void main(String[] args) {
        MiniWeb.run(Application.class);
    }
}
```

### scanBasePackages

Specifies the base package in which MiniWeb searches for controllers.

If omitted or left empty, the package containing the application class is used.

Controllers must be located within the configured scanning scope.

### host

Specifies the network interface address on which the server listens.

- `127.0.0.1`: accepts local connections.
- `0.0.0.0`: listens on all available network interfaces.

### port

Specifies the TCP port used by the HTTP server.

The default port is `8080`.

### staticDirectory

Specifies the directory used to serve static resources.

The default directory is `static`.

---

# 4. Controller Development

## 4.1 @RestController

`@RestController` marks a class as an HTTP controller.

During application startup, MiniWeb scans the configured packages, discovers annotated controller classes, creates their instances, and registers their mapped methods with the Router.

Example:

```java
package com.example.app.controller;

import com.miniweb.annotation.RestController;
import com.miniweb.annotation.GetMapping;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello MiniWeb!";
    }
}
```

The endpoint can be accessed at:

```text
GET http://localhost:8080/hello
```

Response:

```text
Hello MiniWeb!
```

## 4.2 @GetMapping

`@GetMapping` registers a controller method for HTTP GET requests.

```java
@GetMapping("/users")
public String getUsers() {
    return "User List";
}
```

## 4.3 @PostMapping

`@PostMapping` registers a controller method for HTTP POST requests.

```java
@PostMapping("/users")
public String createUser() {
    return "User Created";
}
```

## 4.4 Multiple Mappings

A controller can contain multiple mapped methods.

```java
@RestController
public class UserController {

    @GetMapping("/users")
    public String getUsers() {
        return "User List";
    }

    @GetMapping("/users/1")
    public String getUser() {
        return "User 1";
    }

    @PostMapping("/users")
    public String createUser() {
        return "User Created";
    }
}
```

A route is identified by its HTTP method and path.

Currently, the annotation-based mapping API includes:

- `@GetMapping`
- `@PostMapping`

Other HTTP methods should not be assumed to have corresponding mapping annotations.

---

# 5. Routing

MiniWeb provides a Router that matches incoming requests against registered routes.

The Router considers both:

- HTTP method
- Request path

## 5.1 Static Routes

A static route matches a predefined path.

```java
@GetMapping("/hello")
public String hello() {
    return "Hello";
}
```

## 5.2 Dynamic Routes

MiniWeb supports dynamic path matching.

For example:

```java
@GetMapping("/users/{id}")
public String getUser() {
    return "User Detail";
}
```

The following paths can match this route:

```text
/users/1
/users/100
/users/alice
```

Dynamic routing matches the URL structure. It should not be confused with automatic Java method parameter binding.

The actual path parameter retrieval mechanism depends on the public `HttpRequest` API.

## 5.3 Route Matching and Errors

The Router distinguishes between two important cases:

**404 Not Found**

No registered route matches the requested path.

**405 Method Not Allowed**

The path exists, but the requested HTTP method is not supported by that route.

For a 405 response, MiniWeb provides an `Allow` response header indicating the supported HTTP methods.

---

# 6. HTTP Request

## 6.1 HttpRequest

`HttpRequest` represents a parsed HTTP request.

It encapsulates request information so that application code does not need to parse raw HTTP data or directly read from a Socket.

An HTTP request contains:

- HTTP method
- Request path
- HTTP version
- Request headers
- Query parameters
- Request body

The request lifecycle is:

```text
Raw HTTP Data
      |
      v
  HttpParser
      |
      v
  HttpRequest
      |
      v
 Controller
```

## 6.2 Query Parameters

MiniWeb parses query parameters from the request URL.

Example:

```http
GET /users?page=1&size=10 HTTP/1.1
Host: localhost:8080
```

The query parameters are part of the parsed `HttpRequest`.

Controller methods can receive request information through the framework's argument resolution mechanism.

When direct access is required, use the actual public methods exposed by `HttpRequest`.

Do not assume Spring MVC annotations such as `@RequestParam` are available.

## 6.3 Request Headers

HTTP request headers are parsed and encapsulated in `HttpRequest`.

Example:

```http
GET /users HTTP/1.1
Host: localhost:8080
Accept: application/json
```

Applications can access request header information through the corresponding `HttpRequest` API.

## 6.4 Request Body

MiniWeb parses HTTP request bodies according to the request headers and encapsulates the result in `HttpRequest`.

Example:

```http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: ...

{
    "username": "Alice",
    "age": 20
}
```

The parser supports `Content-Length`-based request body handling.

The maximum supported request body size is currently 10 MB.

---

# 7. Argument Resolution and Type Conversion

MiniWeb separates controller argument processing into dedicated components.

| Component | Responsibility |
|---|---|
| `ArgumentResolver` | Resolves controller method arguments from HTTP requests |
| `TypeConverter` | Converts values into Java types |
| `JsonConverter` | Converts JSON data into Java objects |

These components work together when a controller method is invoked.

Conceptually:

```text
HttpRequest
    |
    v
ArgumentResolver
    |
    +---- Request Information
    |
    +---- TypeConverter
    |
    +---- JsonConverter
    |
    v
Controller Method Arguments
```

The exact supported argument types depend on the current implementation of `ArgumentResolver` and `TypeConverter`.

Do not assume that MiniWeb supports every Spring MVC parameter annotation or binding convention.

---

# 8. JSON Conversion

MiniWeb integrates Jackson for JSON serialization and deserialization.

The framework provides:

- `JsonConverter`
- `ReturnValueHandler`

These components are responsible for JSON conversion and controller return value processing.

## 8.1 Java Object to JSON

A controller can return a Java object:

```java
@GetMapping("/users")
public User getUser() {
    return new User(1, "Alice", 20);
}
```

When the return type is supported by `ReturnValueHandler`, MiniWeb converts the object into JSON.

Example response:

```json
{
    "id": 1,
    "username": "Alice",
    "age": 20
}
```

## 8.2 Java Model

```java
package com.example.app.model;

public class User {

    private int id;
    private String username;
    private int age;

    public User() {
    }

    public User(int id, String username, int age) {
        this.id = id;
        this.username = username;
        this.age = age;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
```

JSON request deserialization depends on the argument types supported by `ArgumentResolver`.

---

# 9. HTTP Response

## 9.1 HttpResponse

`HttpResponse` encapsulates the HTTP response sent to the client.

An HTTP response consists of:

- HTTP version
- Status code
- Reason phrase
- Response headers
- Response body

The framework uses `ReturnValueHandler` to process controller return values and construct the corresponding response.

## 9.2 Controller Return Values

The basic supported return value patterns include:

**String response**

```java
@GetMapping("/hello")
public String hello() {
    return "Hello MiniWeb!";
}
```

**Object response**

```java
@GetMapping("/user")
public User getUser() {
    return new User(1, "Alice", 20);
}
```

The object response is converted using the JSON conversion mechanism when supported by the return value handler.

## 9.3 HTTP Status Codes

MiniWeb provides `HttpStatus` to represent HTTP status codes.

The routing layer handles the following important cases:

| Status | Meaning |
|---|---|
| 200 | Successful request |
| 404 | Resource or route not found |
| 405 | Method not allowed |

The framework's response behavior for other status codes depends on the corresponding response handling logic.

---

# 10. Static Resource Handling

MiniWeb provides static resource serving through `StaticResourceHandler`.

The default static resource directory is:

```text
static/
```

Example project structure:

```text
my-backend/
├── pom.xml
├── static/
│   ├── index.html
│   ├── css/
│   │   └── style.css
│   ├── js/
│   │   └── app.js
│   └── images/
│       └── logo.png
└── src/
    └── main/
        └── java/
```

Resources can be accessed through HTTP:

```text
http://localhost:8080/index.html
http://localhost:8080/css/style.css
http://localhost:8080/js/app.js
```

To use another directory:

```java
@MiniWebApplication(
    staticDirectory = "public"
)
public class Application {

    public static void main(String[] args) {
        MiniWeb.run(Application.class);
    }
}
```

Static resource handling is separate from controller routing.

The directory is resolved relative to the application's runtime working directory.

---

# 11. HTTP Protocol and Connection Management

MiniWeb implements HTTP protocol processing on top of TCP sockets.

Current protocol capabilities include:

- HTTP/1.0
- HTTP/1.1
- Request line parsing
- Header parsing
- `Content-Length` request body parsing
- CRLF protocol line handling
- Keep-Alive
- Query parameter parsing
- Request body size limitation

## 11.1 Keep-Alive

MiniWeb supports persistent HTTP connections.

A client can reuse a TCP connection to send subsequent HTTP requests when the connection remains open.

Connection management is handled internally by the framework.

## 11.2 Socket Timeout

The current client socket read timeout is 30 seconds.

If a client connection exceeds this timeout while waiting for data, the connection is closed and the event is logged.

## 11.3 Connection Lifecycle

Each accepted client connection is handled by a `ClientHandler`.

The handler delegates protocol processing to `HttpProtocolHandler` and closes the socket when request processing finishes or an error occurs.

Application controllers do not need to manage Socket lifecycles.

---

# 12. Concurrency

MiniWeb uses a fixed-size thread pool to process client connections.

Current default configuration:

| Configuration | Value |
|---|---:|
| Worker Threads | 10 |
| Queue Capacity | 100 |
| Rejection Policy | `AbortPolicy` |

When all worker threads are busy and the task queue is full, new connection tasks are rejected.

The corresponding client socket is closed.

This provides bounded resource usage and prevents an unlimited number of pending connection tasks.

The thread pool is managed internally by MiniWeb.

---

# 13. Logging

MiniWeb uses SLF4J as its logging API.

The framework records important lifecycle and request processing events, including:

- Server startup and shutdown
- Client connections
- HTTP requests
- Controller registration
- Route registration
- Socket timeout
- Connection rejection
- Request processing errors

Application code can also use SLF4J:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class UserController {

    private static final Logger log =
            LoggerFactory.getLogger(UserController.class);

    @GetMapping("/users")
    public String getUsers() {
        log.info("Getting users");
        return "User List";
    }
}
```

SLF4J is a logging facade. The application is responsible for providing a logging implementation if actual log output is required.

MiniWeb does not require the application to use a specific logging implementation.

---

# 14. Error Handling

MiniWeb handles errors at different stages of request processing.

## 14.1 Routing Errors

The Router distinguishes between:

- 404 Not Found
- 405 Method Not Allowed

## 14.2 HTTP Parsing Errors

`HttpParser` is responsible for parsing HTTP requests.

Malformed request data is handled by the protocol processing layer.

## 14.3 Connection Errors

Socket-related errors, client disconnects, and timeouts are handled by the server connection layer.

## 14.4 Controller Exceptions

Controller methods may throw exceptions during business processing.

Business logic should handle expected application errors appropriately.

A complete Spring-style global exception handling mechanism should not be assumed unless explicitly implemented.

---

# 15. Complete Example

This section combines the basic framework features into a small application.

## 15.1 Project Structure

```text
my-backend/
├── pom.xml
├── static/
│   └── index.html
└── src/
    └── main/
        └── java/
            └── com/example/app/
                ├── Application.java
                ├── controller/
                │   └── UserController.java
                └── model/
                    └── User.java
```

## 15.2 Application.java

```java
package com.example.app;

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

## 15.3 User.java

```java
package com.example.app.model;

public class User {

    private int id;
    private String username;
    private int age;

    public User() {
    }

    public User(int id, String username, int age) {
        this.id = id;
        this.username = username;
        this.age = age;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
```

## 15.4 UserController.java

```java
package com.example.app.controller;

import com.miniweb.annotation.RestController;
import com.miniweb.annotation.GetMapping;
import com.miniweb.annotation.PostMapping;
import com.example.app.model.User;

@RestController
public class UserController {

    @GetMapping("/users")
    public User getUser() {
        return new User(1, "Alice", 20);
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello MiniWeb!";
    }

    @PostMapping("/users")
    public String createUser() {
        return "User Created";
    }
}
```

## 15.5 Run the Application

Run the `Application.main()` method from your IDE.

Alternatively, use Maven Exec Plugin in the application project:

```bash
mvn exec:java -Dexec.mainClass="com.example.app.Application"
```

The Exec Plugin should be configured in the application project if it is not already available.

## 15.6 Test the Endpoints

GET:

```bash
curl http://localhost:8080/hello
```

Expected response:

```text
Hello MiniWeb!
```

GET:

```bash
curl http://localhost:8080/users
```

Expected response:

```json
{
    "id": 1,
    "username": "Alice",
    "age": 20
}
```

POST:

```bash
curl -X POST http://localhost:8080/users
```

Expected response:

```text
User Created
```

Test a missing route:

```bash
curl -i http://localhost:8080/not-found
```

Test an unsupported method:

```bash
curl -i -X DELETE http://localhost:8080/hello
```

---

# 16. Framework API Reference

The following table summarizes the main public classes and annotations.

| API | Package | Responsibility |
|---|---|---|
| `MiniWeb` | `com.miniweb` | Application startup and logger access |
| `@MiniWebApplication` | `com.miniweb.annotation` | Application configuration |
| `@RestController` | `com.miniweb.annotation` | Controller declaration |
| `@GetMapping` | `com.miniweb.annotation` | GET route mapping |
| `@PostMapping` | `com.miniweb.annotation` | POST route mapping |
| `HttpRequest` | `com.miniweb.http` | HTTP request representation |
| `HttpResponse` | `com.miniweb.http` | HTTP response representation |
| `HttpMethod` | `com.miniweb.http` | HTTP method representation |
| `HttpStatus` | `com.miniweb.http` | HTTP status representation |
| `HttpParser` | `com.miniweb.http` | HTTP request parsing |
| `JsonConverter` | `com.miniweb.controller` | JSON conversion |
| `ArgumentResolver` | `com.miniweb.controller` | Controller argument resolution |
| `TypeConverter` | `com.miniweb.controller` | Type conversion |
| `ReturnValueHandler` | `com.miniweb.controller` | Controller return value handling |
| `Router` | `com.miniweb.routing` | Route registration and matching |
| `Dispatcher` | `com.miniweb.routing` | Request dispatching |
| `MiniWebServer` | `com.miniweb.server` | TCP server |
| `ClientHandler` | `com.miniweb.server` | Client connection handling |
| `ProtocolHandler` | `com.miniweb.protocol` | Protocol processing abstraction |
| `HttpProtocolHandler` | `com.miniweb.http` | HTTP protocol processing |

---

# 17. Current Scope

MiniWeb currently focuses on the fundamental infrastructure of a Java Web framework.

The following features are not part of the current documented application API:

- IoC container and dependency injection
- Automatic Service or Repository registration
- Spring-style `@RequestParam` and `@PathVariable`
- Spring-style global exception handling
- Database connection management
- ORM integration
- Authentication and authorization
- WebSocket
- HTTPS
- File upload
- Middleware and interceptor infrastructure

These features may be considered for future development.

---

# 18. Summary

MiniWeb provides a lightweight way to build Java Web applications.

A basic application only needs three steps:

1. Add MiniWeb as a Maven dependency.
2. Create an application class annotated with `@MiniWebApplication`.
3. Create controllers using `@RestController`, `@GetMapping`, and `@PostMapping`.

MiniWeb handles the underlying server, HTTP protocol processing, request parsing, route dispatching, response generation, and connection management.

**Write your controllers and build your application. MiniWeb handles the HTTP infrastructure.**