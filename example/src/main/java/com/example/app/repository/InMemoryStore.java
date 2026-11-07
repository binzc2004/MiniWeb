package com.example.app.repository;

import com.example.app.model.Product;
import com.example.app.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内存数据仓库。
 *
 * <p>示例项目没有真实数据库，因此使用 {@link ConcurrentHashMap} 在内存中保存数据，
 * 用于演示框架的 POST 写入能力以及后续 GET 读取能力。
 *
 * <p>注意：这是示例代码，不是框架的一部分。数据仅在 JVM 进程存活期间有效。
 */
public final class InMemoryStore {

    private static final InMemoryStore INSTANCE = new InMemoryStore();

    private final Map<Integer, User> users = new ConcurrentHashMap<>();
    private final Map<Integer, Product> products = new ConcurrentHashMap<>();

    private final AtomicInteger userSeq = new AtomicInteger(0);
    private final AtomicInteger productSeq = new AtomicInteger(0);

    private InMemoryStore() {
        seed();
    }

    public static InMemoryStore getInstance() {
        return INSTANCE;
    }

    /**
     * 预置一些假数据，方便 GET 接口在没有先 POST 的情况下也能返回内容。
     */
    private void seed() {
        saveUser(new User(0, "Alice", 20, "alice@miniweb.dev"));
        saveUser(new User(0, "Bob", 24, "bob@miniweb.dev"));
        saveUser(new User(0, "Carol", 31, "carol@miniweb.dev"));

        saveProduct(new Product(0, "Mechanical Keyboard", 599.0, 12, true,
                new Product.Category(1, "Peripherals")));
        saveProduct(new Product(0, "27-inch Monitor", 1499.0, 5, true,
                new Product.Category(1, "Peripherals")));
        saveProduct(new Product(0, "USB-C Hub", 199.0, 0, false,
                new Product.Category(2, "Accessories")));
    }

    // ------------------------------------------------------------------
    // User
    // ------------------------------------------------------------------

    public User saveUser(User user) {
        if (user.getId() <= 0) {
            user.setId(userSeq.incrementAndGet());
        } else {
            userSeq.accumulateAndGet(user.getId(), Math::max);
        }
        users.put(user.getId(), user);
        return user;
    }

    public Optional<User> findUser(int id) {
        return Optional.ofNullable(users.get(id));
    }

    public List<User> findAllUsers() {
        List<User> list = new ArrayList<>(users.values());
        list.sort(Comparator.comparingInt(User::getId));
        return list;
    }

    public boolean deleteUser(int id) {
        return users.remove(id) != null;
    }

    public int countUsers() {
        return users.size();
    }

    // ------------------------------------------------------------------
    // Product
    // ------------------------------------------------------------------

    public Product saveProduct(Product product) {
        if (product.getId() <= 0) {
            product.setId(productSeq.incrementAndGet());
        } else {
            productSeq.accumulateAndGet(product.getId(), Math::max);
        }
        products.put(product.getId(), product);
        return product;
    }

    public Optional<Product> findProduct(int id) {
        return Optional.ofNullable(products.get(id));
    }

    public List<Product> findAllProducts() {
        List<Product> list = new ArrayList<>(products.values());
        list.sort(Comparator.comparingInt(Product::getId));
        return list;
    }

    public boolean deleteProduct(int id) {
        return products.remove(id) != null;
    }
}
