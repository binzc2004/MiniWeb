package com.example.app.model;

/**
 * 商品实体。
 *
 * <p>用于演示框架同时注册多个 Controller、以及更复杂的字段类型（double / boolean / 嵌套对象）的 JSON 转换能力。
 */
public class Product {

    private int id;
    private String name;
    private double price;
    private int stock;
    private boolean available;
    private Category category;

    public Product() {
    }

    public Product(int id, String name, double price, int stock, boolean available, Category category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.available = available;
        this.category = category;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * 嵌套分类对象，用于演示多层对象的 JSON 序列化。
     */
    public static class Category {

        private int id;
        private String name;

        public Category() {
        }

        public Category(int id, String name) {
            this.id = id;
            this.name = name;
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
