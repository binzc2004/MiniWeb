package com.example.app.model;

/**
 * 用户实体。
 *
 * <p>用于演示框架的 JSON 序列化与反序列化能力：
 * <ul>
 *     <li>Controller 返回该对象时由 JSON 转换组件序列化为 JSON 响应</li>
 *     <li>Controller 以该类型作为参数时由参数解析组件从 JSON 请求体反序列化</li>
 * </ul>
 */
public class User {

    private int id;
    private String username;
    private int age;
    private String email;

    public User() {
    }

    public User(int id, String username, int age, String email) {
        this.id = id;
        this.username = username;
        this.age = age;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', age=" + age
                + ", email='" + email + "'}";
    }
}
