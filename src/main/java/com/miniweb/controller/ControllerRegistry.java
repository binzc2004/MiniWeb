
package com.miniweb.controller;

import com.miniweb.annotation.GetMapping;
import com.miniweb.annotation.PostMapping;
import com.miniweb.http.HttpMethod;
import com.miniweb.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Controller 注册器。
 *
 * 负责实例化 Controller，并将带有映射注解的方法注册到 Router。
 *
 * @author zicheng bin
 */
public class ControllerRegistry {

    private static final Logger log = LoggerFactory.getLogger(ControllerRegistry.class);

    private final Router router;
    private final ArgumentResolver argumentResolver;
    private final ReturnValueHandler returnValueHandler;

    public ControllerRegistry(
            Router router,
            ArgumentResolver argumentResolver,
            ReturnValueHandler returnValueHandler) {
        if (router == null || argumentResolver == null
                || returnValueHandler == null) {
            throw new IllegalArgumentException(
                    "ControllerRegistry dependencies cannot be null");
        }

        this.router = router;
        this.argumentResolver = argumentResolver;
        this.returnValueHandler = returnValueHandler;
    }

    /**
     * 扫描指定包并注册其中的 Controller。
     *
     * @param basePackage Controller 扫描包名
     */
    public void registerControllers(String basePackage) {
        ControllerScanner scanner = new ControllerScanner(basePackage);
        Set<Class<?>> controllerClasses = scanner.scan();

        int registeredCount = 0;

        for (Class<?> controllerClass : controllerClasses) {
            Object controller = createController(controllerClass);

            for (Method method : controllerClass.getDeclaredMethods()) {
                HttpMethod httpMethod = resolveHttpMethod(method);

                if (httpMethod == null) {
                    continue;
                }

                String path = resolvePath(method);

                HandlerMethod handlerMethod = new HandlerMethod(
                        controller,
                        method,
                        argumentResolver,
                        returnValueHandler);

                router.addRoute(httpMethod, path, handlerMethod);

                registeredCount++;

                log.debug(
                        "Registered route: {} {} -> {}.{}",
                        httpMethod,
                        path,
                        controllerClass.getSimpleName(),
                        method.getName());
            }
        }

        log.info(
                "Controller registration completed, registered {} routes",
                registeredCount);
    }

    /**
     * 创建 Controller 实例。
     */
    private Object createController(Class<?> controllerClass) {
        try {
            if (Modifier.isAbstract(controllerClass.getModifiers())) {
                throw new IllegalStateException(
                        "Controller cannot be abstract: "
                                + controllerClass.getName());
            }

            Constructor<?> constructor = controllerClass.getDeclaredConstructor();

            constructor.setAccessible(true);

            return constructor.newInstance();

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to create Controller: "
                            + controllerClass.getName(),
                    e);
        }
    }

    /**
     * 根据方法注解确定 HTTP Method。
     */
    private HttpMethod resolveHttpMethod(Method method) {
        boolean getMapping = method.isAnnotationPresent(GetMapping.class);
        boolean postMapping = method.isAnnotationPresent(PostMapping.class);

        if (getMapping && postMapping) {
            throw new IllegalStateException(
                    "Controller method cannot have multiple mapping annotations: "
                            + method.getName());
        }

        if (getMapping) {
            return HttpMethod.GET;
        }

        if (postMapping) {
            return HttpMethod.POST;
        }

        return null;
    }

    /**
     * 获取方法映射路径。
     */
    private String resolvePath(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            return method.getAnnotation(GetMapping.class).value();
        }

        return method.getAnnotation(PostMapping.class).value();
    }
}
