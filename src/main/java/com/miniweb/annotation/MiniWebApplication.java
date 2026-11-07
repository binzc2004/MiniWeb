package com.miniweb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MiniWeb 应用启动注解。
 *
 * @author zicheng bin
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MiniWebApplication {

    /**
     * Controller 扫描包。
     * 为空时默认使用启动类所在的包。
     */
    String scanBasePackages() default "";

    /**
     * 服务器监听地址。
     */
    String host() default "127.0.0.1";

    /**
     * 服务器监听端口。
     */
    int port() default 8080;

    /**
     * 静态资源根目录。
     */
    String staticDirectory() default "static";
}