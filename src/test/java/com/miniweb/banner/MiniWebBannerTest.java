
package com.miniweb.banner;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MiniWebBannerTest {

    @Test
    void shouldPrintBanner() {
        // 捕获控制台输出
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

            MiniWebBanner.print();

            String result = output.toString(StandardCharsets.UTF_8);

            assertFalse(result.isBlank());
            assertTrue(result.contains("MiniWeb"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
