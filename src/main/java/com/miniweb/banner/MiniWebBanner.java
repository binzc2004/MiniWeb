
package com.miniweb.banner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MiniWebBanner {

    private static final String FILE_NAME = "banner.txt";

    public static void print() {
        try (InputStream inputStream = MiniWebBanner.class
                .getClassLoader()
                .getResourceAsStream(FILE_NAME)) {

            if (inputStream == null) {
                System.out.println("MiniWeb Banner not found.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

        } catch (IOException e) {
            System.err.println("Failed to load MiniWeb Banner: " + e.getMessage());
        }
    }
}
