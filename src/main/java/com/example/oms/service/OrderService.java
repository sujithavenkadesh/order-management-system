package com.example.oms.service;

import com.example.oms.dto.OrderItemResponse;
import com.example.oms.dto.OrderResponse;
import com.example.oms.dto.PaymentRequest;
import com.example.oms.dto.PaymentResponse;
import com.example.oms.entity.Cart;
import com.example.oms.entity.CartItem;
import com.example.oms.entity.Order;
import com.example.oms.entity.OrderItem;
import com.example.oms.entity.OrderStatus;
import com.example.oms.entity.Payment;
import com.example.oms.entity.PaymentStatus;
import com.example.oms.entity.Product;
import com.example.oms.entity.User;
import com.example.oms.exception.BadRequestException;
import com.example.oms.exception.ResourceNotFoundException;
import com.example.oms.repository.CartRepository;
import com.example.oms.repository.OrderRepository;
import com.example.oms.repository.ProductRepository;
import com.example.oms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse placeOrder(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user: " + userId));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PLACED);

        List<CartItem> cartItems = cart.getItems().stream()
                .sorted(Comparator.comparing((CartItem i) -> i.getProduct().getId()))
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            int qty = cartItem.getQuantity();

            int updated = productRepository.reduceStock(product.getId(), qty);
            if (updated == 0) {
                throw new BadRequestException("Insufficient stock for " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(qty);
            orderItem.setPriceAtPurchase(product.getPrice());
            order.getItems().add(orderItem);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        cart.getItems().clear();
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return toResponse(findWithDetails(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse pay(Long orderId, PaymentRequest request) {
        Order order = findWithDetails(orderId);
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BadRequestException(
                    "Order cannot be paid. Current status: " + order.getStatus());
        }

        boolean simulateFailure = request != null && Boolean.TRUE.equals(request.simulateFailure());

        Payment payment = order.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
            order.setPayment(payment);
        }
        payment.setAmount(order.getTotalAmount());

        if (simulateFailure) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaidAt(null);
        } else {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            order.setStatus(OrderStatus.PAID);
        }

        orderRepository.flush();
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = findWithDetails(orderId);
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BadRequestException(
                    "Only PLACED orders can be cancelled. Current status: " + order.getStatus());
        }

        for (OrderItem item : order.getItems()) {
            productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus target) {
        Order order = findWithDetails(orderId);
        OrderStatus current = order.getStatus();

        boolean allowed = (current == OrderStatus.PAID && target == OrderStatus.SHIPPED)
                || (current == OrderStatus.SHIPPED && target == OrderStatus.DELIVERED);
        if (!allowed) {
            throw new BadRequestException("Invalid status change: " + current + " -> " + target);
        }

        order.setStatus(target);
        return toResponse(order);
    }

    private Order findWithDetails(Long orderId) {
        return orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private OrderResponse toResponse(Order o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getPriceAtPurchase(),
                        i.getPriceAtPurchase().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();

        PaymentResponse payment = o.getPayment() == null ? null
                : new PaymentResponse(o.getPayment().getId(), o.getPayment().getAmount(),
                        o.getPayment().getStatus(), o.getPayment().getPaidAt());

        return new OrderResponse(o.getId(), o.getUser().getId(), o.getStatus(),
                o.getTotalAmount(), o.getCreatedAt(), items, payment);
    }
}