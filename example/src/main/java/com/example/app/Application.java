package com.example.app;

import com.miniweb.MiniWeb;
import com.miniweb.annotation.MiniWebApplication;

/**
 * MiniWeb 示例应用启动类。
 *
 * <p>覆盖的框架配置能力：
 * <ul>
 *     <li>{@code scanBasePackages} —— Controller 扫描基础包</li>
 *     <li>{@code host} —— 监听地址</li>
 *     <li>{@code port} —— 监听端口</li>
 *     <li>{@code staticDirectory} —— 静态资源目录</li>
 * </ul>
 *
 * <p>启动方式：
 * <pre>
 *   mvn exec:java -Dexec.mainClass="com.example.app.Application"
 *   # 或者直接在 IDE 中运行 main 方法
 * </pre>
 */
@MiniWebApplication(
        scanBasePackages = "com.example.app",
        host = "127.0.0.1",
        port = 8080,
        staticDirectory = "static"
)
public class Application {

    public static void main(String[] args) {
        MiniWeb.run(Application.class);
    }
}
