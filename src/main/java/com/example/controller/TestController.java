
package com.example.controller;

import com.miniweb.annotation.GetMapping;
import com.miniweb.annotation.PostMapping;
import com.miniweb.annotation.PathVariable;
import com.miniweb.annotation.RequestParam;
import com.miniweb.annotation.RequestBody;
import com.miniweb.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello MiniWeb!";
    }

    @GetMapping("/user/{id}")
    public String getUser(@PathVariable("id") int id) {
        return "User ID: " + id;
    }

    @GetMapping("/search")
    public String search(@RequestParam("name") String name) {
        return "Search: " + name;
    }

    @GetMapping("/user/detail/{id}")
    public Map<String, Object> getUserDetail(
            @PathVariable("id") int id,
            @RequestParam("name") String name) {
        return Map.of("id", id, "name", name);
    }

    @PostMapping("/user")
    public Map<String, Object> createUser(@RequestBody User user) {
        return Map.of(
                "id", user.getId(),
                "name", user.getName());
    }

    @PostMapping("/echo")
    public String echo(@RequestBody String body) {
        return body;
    }

    public static class User {
        private int id;
        private String name;

        public User() {
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
