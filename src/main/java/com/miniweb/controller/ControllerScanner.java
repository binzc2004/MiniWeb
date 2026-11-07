
package com.miniweb.controller;

import com.miniweb.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Controller 扫描器。
 *
 * @author zicheng bin
 */
public class ControllerScanner {

    private static final Logger log =
            LoggerFactory.getLogger(ControllerScanner.class);

    private final String basePackage;
    private final ClassLoader classLoader;

    public ControllerScanner(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            throw new IllegalArgumentException(
                    "Base package cannot be empty"
            );
        }

        this.basePackage = basePackage;
        this.classLoader = Thread.currentThread().getContextClassLoader()
                != null
                ? Thread.currentThread().getContextClassLoader()
                : ControllerScanner.class.getClassLoader();
    }

    /**
     * 扫描指定包下所有带有 @RestController 的类。
     */
    public Set<Class<?>> scan() {
        Set<Class<?>> controllers = new LinkedHashSet<>();

        String packagePath = basePackage.replace('.', '/');

        try {
            Enumeration<URL> resources =
                    classLoader.getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if ("file".equals(resource.getProtocol())) {
                    scanDirectory(resource, controllers);
                } else if ("jar".equals(resource.getProtocol())) {
                    scanJar(resource, packagePath, controllers);
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(
                    "Failed to scan package: " + basePackage,
                    e
            );
        }

        log.debug(
                "Controller scanning completed, found {} controllers in {}",
                controllers.size(),
                basePackage
        );

        return Collections.unmodifiableSet(controllers);
    }

    /**
     * 扫描文件系统中的包目录。
     */
    private void scanDirectory(
            URL resource,
            Set<Class<?>> controllers
    ) throws IOException, URISyntaxException {
        Path directory = Paths.get(resource.toURI());

        if (!Files.isDirectory(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> {
                        String relativePath = directory.relativize(path)
                                .toString()
                                .replace('\\', '.')
                                .replace('/', '.');

                        String className = basePackage + "."
                                + relativePath.substring(
                                        0,
                                        relativePath.length() - 6
                                );

                        addController(className, controllers);
                    });
        }
    }

    /**
     * 扫描 JAR 包中的类。
     */
    private void scanJar(
            URL resource,
            String packagePath,
            Set<Class<?>> controllers
    ) throws IOException {
        JarURLConnection connection =
                (JarURLConnection) resource.openConnection();

        connection.setUseCaches(false);

        try (JarFile jarFile = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entry.isDirectory()
                        || !entryName.startsWith(packagePath + "/")
                        || !entryName.endsWith(".class")) {
                    continue;
                }

                String className = entryName
                        .substring(0, entryName.length() - 6)
                        .replace('/', '.');

                addController(className, controllers);
            }
        }
    }

    /**
     * 加载类并判断是否为 Controller。
     */
    private void addController(
            String className,
            Set<Class<?>> controllers
    ) {
        if (className.endsWith("package-info")
                || className.endsWith("module-info")) {
            return;
        }

        try {
            Class<?> clazz = Class.forName(
                    className,
                    false,
                    classLoader
            );

            if (clazz.isAnnotationPresent(RestController.class)) {
                controllers.add(clazz);

                log.debug("Found Controller: {}", className);
            }
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IllegalStateException(
                    "Failed to load class: " + className,
                    e
            );
        }
    }
}
