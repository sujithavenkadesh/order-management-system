package com.example.oms;

import com.example.oms.entity.Cart;
import com.example.oms.entity.CartItem;
import com.example.oms.entity.Order;
import com.example.oms.entity.OrderItem;
import com.example.oms.entity.OrderStatus;
import com.example.oms.entity.Product;
import com.example.oms.entity.User;

import java.math.BigDecimal;

public final class TestData {

    private TestData() {
    }

    public static User user(long id) {
        User u = new User();
        u.setId(id);
        u.setName("Test User");
        u.setEmail("user" + id + "@example.com");
        return u;
    }

    public static Product product(long id, String name, String price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStockQty(stock);
        return p;
    }

    public static Cart cart(User user) {
        Cart c = new Cart();
        c.setId(user.getId());
        c.setUser(user);
        return c;
    }

    public static CartItem addToCart(Cart cart, Product product, int qty) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(qty);
        cart.getItems().add(item);
        return item;
    }

    public static Order order(long id, User user, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setUser(user);
        o.setStatus(status);
        o.setTotalAmount(BigDecimal.ZERO);
        return o;
    }

    public static OrderItem addToOrder(Order order, Product product, int qty) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(qty);
        item.setPriceAtPurchase(product.getPrice());
        order.getItems().add(item);
        order.setTotalAmount(order.getTotalAmount()
                .add(product.getPrice().multiply(BigDecimal.valueOf(qty))));
        return item;
    }
}