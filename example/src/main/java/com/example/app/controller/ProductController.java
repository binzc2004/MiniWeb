package com.example.app.controller;

import com.example.app.model.ApiResponse;
import com.example.app.model.Product;
import com.example.app.repository.InMemoryStore;
import com.miniweb.annotation.GetMapping;
import com.miniweb.annotation.PostMapping;
import com.miniweb.annotation.RestController;
import com.miniweb.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 商品接口。
 *
 * <p>演示的能力：
 * <ul>
 *     <li>第二个 {@code @RestController} 被扫描注册，验证多 Controller 支持</li>
 *     <li>泛型包装对象 {@link ApiResponse} 的 JSON 序列化</li>
 *     <li>嵌套对象 {@link Product.Category} 的 JSON 序列化</li>
 *     <li>double / boolean 字段的类型处理</li>
 *     <li>请求头驱动的简单业务分支</li>
 * </ul>
 */
@RestController
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final InMemoryStore store = InMemoryStore.getInstance();

    /**
     * 全量商品列表，用 ApiResponse 包装，验证泛型对象的 JSON 化。
     */
    @GetMapping("/products")
    public ApiResponse<List<Product>> getProducts() {
        List<Product> products = store.findAllProducts();
        log.info("Returning {} products", products.size());
        return ApiResponse.ok("product list", products);
    }

    /**
     * 动态路径匹配商品详情。
     */
    @GetMapping("/products/{id}")
    public ApiResponse<Product> getProductById(HttpRequest request) {
        String path = request.getPath();
        String rawId = path.substring(path.lastIndexOf('/') + 1);

        try {
            int id = Integer.parseInt(rawId);
            Optional<Product> product = store.findProduct(id);
            return product
                    .map(p -> ApiResponse.ok("found", p))
                    .orElseGet(() -> ApiResponse.fail("product not found: " + id));
        } catch (NumberFormatException e) {
            return ApiResponse.fail("invalid product id: " + rawId);
        }
    }

    /**
     * 创建商品，JSON 请求体反序列化为 {@link Product}（含嵌套 Category）。
     */
    @PostMapping("/products")
    public ApiResponse<Product> createProduct(Product product) {
        if (product == null) {
            return ApiResponse.fail("empty request body");
        }
        Product saved = store.saveProduct(product);
        log.info("Created product id={}, name={}", saved.getId(), saved.getName());
        return ApiResponse.ok("created", saved);
    }

    /**
     * 只返回商品的库存信息，验证 Map 返回值的 JSON 序列化。
     */
    @GetMapping("/products/{id}/stock")
    public Map<String, Object> getStock(HttpRequest request) {
        String path = request.getPath();
        // path 形如 /products/{id}/stock
        String[] segments = path.split("/");
        String rawId = segments.length >= 3 ? segments[2] : null;

        Optional<Product> product = rawId == null
                ? Optional.empty()
                : store.findProduct(parseIntOrDefault(rawId, -1));

        if (product.isEmpty()) {
            return Map.of("found", false, "id", String.valueOf(rawId));
        }

        Product p = product.get();
        return Map.of(
                "found", true,
                "id", p.getId(),
                "name", p.getName(),
                "stock", p.getStock(),
                "available", p.isAvailable()
        );
    }

    /**
     * 统计接口，演示"没有数据也要返回结构化假数据"的写法。
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(HttpRequest request) {
        List<Product> products = store.findAllProducts();

        int totalStock = 0;
        double totalValue = 0.0;
        int availableCount = 0;
        for (Product p : products) {
            totalStock += p.getStock();
            totalValue += p.getPrice() * p.getStock();
            if (p.isAvailable()) {
                availableCount++;
            }
        }

        Map<String, Object> data = Map.of(
                "userCount", store.countUsers(),
                "productCount", products.size(),
                "availableProducts", availableCount,
                "totalStock", totalStock,
                "totalInventoryValue", Math.round(totalValue * 100) / 100.0,
                "clientAgent", headerOr(request, "User-Agent", "unknown")
        );
        return ApiResponse.ok("statistics", data);
    }

    private static String headerOr(HttpRequest request, String name, String defaultValue) {
        // getHeader 不区分大小写，不存在时返回 null
        String value = request.getHeader(name);
        return value == null ? defaultValue : value;
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
