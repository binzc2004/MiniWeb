package com.example;

import com.miniweb.MiniWeb;
import com.miniweb.annotation.MiniWebApplication;

/**
 * MiniWeb 示例应用。
 */
@MiniWebApplication
public class Application {

    public static void main(String[] args) {
        MiniWeb.run(Application.class);
    }
}