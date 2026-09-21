package com.example.oms.config;

import com.example.oms.entity.Order;
import com.example.oms.entity.OrderItem;
import com.example.oms.entity.OrderStatus;
import com.example.oms.entity.Product;
import com.example.oms.entity.Role;
import com.example.oms.entity.User;
import com.example.oms.repository.OrderRepository;
import com.example.oms.repository.ProductRepository;
import com.example.oms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Component
@Profile("demo")
@org.springframework.core.annotation.Order(2)
@RequiredArgsConstructor
public class ReportDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("asha@example.com")) {
            return;
        }

        User asha = saveUser("Asha", "asha@example.com");
        User bala = saveUser("Bala", "bala@example.com");
        User chitra = saveUser("Chitra", "chitra@example.com");

        // line = {productId, quantity}
        createOrder(asha, OrderStatus.DELIVERED, 3, new int[]{1, 1}, new int[]{2, 2});
        createOrder(asha, OrderStatus.DELIVERED, 2, new int[]{4, 1}, new int[]{3, 3});
        createOrder(asha, OrderStatus.PAID, 1, new int[]{25, 2}, new int[]{5, 1});
        createOrder(bala, OrderStatus.DELIVERED, 2, new int[]{2, 4}, new int[]{6, 2});
        createOrder(bala, OrderStatus.SHIPPED, 1, new int[]{7, 1});
        createOrder(bala, OrderStatus.PAID, 0, new int[]{25, 3}, new int[]{8, 1});
        createOrder(chitra, OrderStatus.DELIVERED, 1, new int[]{3, 5}, new int[]{9, 2});
        createOrder(chitra, OrderStatus.CANCELLED, 1, new int[]{1, 2});
        createOrder(chitra, OrderStatus.PLACED, 0, new int[]{4, 1});
        createOrder(asha, OrderStatus.PAID, 0, new int[]{2, 3}, new int[]{25, 1});
    }

    private User saveUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Pass@1234"));
        user.setRole(Role.CUSTOMER);
        return userRepository.save(user);
    }

    private void createOrder(User user, OrderStatus status, int monthsAgo, int[]... lines) {
        Order order = new Order();
        order.setUser(user);
        order.setStatus(status);

        BigDecimal total = BigDecimal.ZERO;
        for (int[] line : lines) {
            Product product = productRepository.findById((long) line[0]).orElseThrow();
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(line[1]);
            item.setPriceAtPurchase(product.getPrice());
            order.getItems().add(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(line[1])));
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        jdbcTemplate.update("UPDATE orders SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMonths(monthsAgo)), saved.getId());
    }
}