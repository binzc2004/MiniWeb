package com.miniweb;

import com.miniweb.annotation.MiniWebApplication;
import com.miniweb.controller.ArgumentResolver;
import com.miniweb.controller.ControllerRegistry;
import com.miniweb.controller.JsonConverter;
import com.miniweb.controller.ReturnValueHandler;
import com.miniweb.controller.TypeConverter;
import com.miniweb.http.HttpProtocolHandler;
import com.miniweb.routing.Dispatcher;
import com.miniweb.routing.Router;
import com.miniweb.server.MiniWebServer;
import com.miniweb.staticresource.StaticResourceHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MiniWeb 框架启动入口。
 *
 * @author zicheng bin
 */
public class MiniWeb {

        private static final Logger log = getLogger(MiniWeb.class);

        /**
         * 获取指定类对应的日志记录器。
         *
         * @param clazz 日志所属的类
         * @return SLF4J Logger
         */
        public static Logger getLogger(Class<?> clazz) {
                if (clazz == null) {
                        throw new IllegalArgumentException("Logger class cannot be null");
                }

                return LoggerFactory.getLogger(clazz);
        }

        /**
         * 启动 MiniWeb 应用。
         *
         * @param applicationClass 应用启动类
         */
        public static void run(Class<?> applicationClass) {
                if (applicationClass == null) {
                        throw new IllegalArgumentException(
                                        "Application class cannot be null");
                }

                MiniWebApplication application = applicationClass.getAnnotation(MiniWebApplication.class);

                if (application == null) {
                        throw new IllegalArgumentException(
                                        "Application class must have @MiniWebApplication");
                }

                String basePackage = application.scanBasePackages().isBlank()
                                ? applicationClass.getPackageName()
                                : application.scanBasePackages().trim();

                if (basePackage.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Controller scan package cannot be empty");
                }

                String host = application.host();
                int port = application.port();
                String staticDirectory = application.staticDirectory();

                if (host == null || host.isBlank()) {
                        throw new IllegalArgumentException("Host cannot be empty");
                }

                if (port < 1 || port > 65535) {
                        throw new IllegalArgumentException("Invalid server port: " + port);
                }

                if (staticDirectory == null || staticDirectory.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Static directory cannot be empty");
                }

                log.info("Starting MiniWeb application: {}",
                                applicationClass.getName());

                log.debug("Application configuration: host={}, port={}, scanPackage={}, staticDirectory={}",
                                host, port, basePackage, staticDirectory);

                Router router = new Router();

                JsonConverter jsonConverter = new JsonConverter();

                ArgumentResolver argumentResolver = new ArgumentResolver(
                                new TypeConverter(),
                                jsonConverter);

                ReturnValueHandler returnValueHandler = new ReturnValueHandler(jsonConverter);

                ControllerRegistry registry = new ControllerRegistry(
                                router,
                                argumentResolver,
                                returnValueHandler);

                registry.registerControllers(basePackage);

                StaticResourceHandler staticResourceHandler = new StaticResourceHandler(staticDirectory);

                Dispatcher dispatcher = new Dispatcher(
                                router,
                                staticResourceHandler);

                HttpProtocolHandler protocolHandler = new HttpProtocolHandler(dispatcher);

                MiniWebServer server = new MiniWebServer(
                                host,
                                port,
                                protocolHandler);

                log.info("MiniWeb application initialized successfully");

                server.start();
        }
}