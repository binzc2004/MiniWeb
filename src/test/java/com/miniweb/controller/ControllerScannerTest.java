
package com.miniweb.controller;

import com.miniweb.annotation.RestController;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ControllerScanner 测试。
 *
 * @author zicheng bin
 */
class ControllerScannerTest {

    @RestController
    static class TestController {
    }

    static class NormalService {
    }

    @Test
    void shouldFindAnnotatedControllers() {
        ControllerScanner scanner = new ControllerScanner("com.miniweb.controller");

        Set<Class<?>> controllers = scanner.scan();

        assertTrue(
                controllers.contains(TestController.class),
                "Should find classes annotated with @RestController");

        assertFalse(
                controllers.contains(NormalService.class),
                "Should ignore classes without @RestController");
    }

    @Test
    void shouldReturnEmptySetWhenNoControllerFound() {
        ControllerScanner scanner = new ControllerScanner("com.miniweb.controller.nonexistent");

        Set<Class<?>> controllers = scanner.scan();

        assertTrue(controllers.isEmpty());
    }

    @Test
    void shouldRejectEmptyPackageName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ControllerScanner(""));
    }
}
