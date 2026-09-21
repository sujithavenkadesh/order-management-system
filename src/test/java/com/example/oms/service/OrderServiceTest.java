package com.example.oms.service;

import com.example.oms.dto.OrderResponse;
import com.example.oms.dto.PaymentRequest;
import com.example.oms.entity.Cart;
import com.example.oms.entity.Order;
import com.example.oms.entity.OrderStatus;
import com.example.oms.entity.PaymentStatus;
import com.example.oms.entity.Product;
import com.example.oms.entity.User;
import com.example.oms.exception.BadRequestException;
import com.example.oms.repository.CartRepository;
import com.example.oms.repository.OrderRepository;
import com.example.oms.repository.ProductRepository;
import com.example.oms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.example.oms.TestData.addToCart;
import static com.example.oms.TestData.addToOrder;
import static com.example.oms.TestData.cart;
import static com.example.oms.TestData.order;
import static com.example.oms.TestData.product;
import static com.example.oms.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrder_success_reducesStockInProductIdOrder_andClearsCart() {
        User user = user(1L);
        Product mouse = product(1L, "Mouse", "599.00", 10);
        Product book = product(2L, "Book", "300.00", 5);
        Cart cart = cart(user);
        addToCart(cart, book, 1);
        addToCart(cart, mouse, 2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.reduceStock(anyLong(), anyInt())).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.placeOrder(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.totalAmount()).isEqualByComparingTo("1498.00");
        assertThat(response.items()).hasSize(2);
        assertThat(cart.getItems()).isEmpty();

        InOrder inOrder = inOrder(productRepository);
        inOrder.verify(productRepository).reduceStock(1L, 2);
        inOrder.verify(productRepository).reduceStock(2L, 1);
    }

    @Test
    void placeOrder_emptyCart_rejected() {
        User user = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart(user)));

        assertThatThrownBy(() -> orderService.placeOrder(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cart is empty");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_insufficientStock_rejected_orderNotSaved_cartKept() {
        User user = user(1L);
        Product mouse = product(1L, "Mouse", "599.00", 1);
        Cart cart = cart(user);
        addToCart(cart, mouse, 2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.reduceStock(1L, 2)).thenReturn(0);

        assertThatThrownBy(() -> orderService.placeOrder(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient stock for Mouse");

        verify(orderRepository, never()).save(any());
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void cancel_placedOrder_restoresStock() {
        Order order = order(10L, user(1L), OrderStatus.PLACED);
        addToOrder(order, product(1L, "Mouse", "599.00", 10), 2);
        addToOrder(order, product(2L, "Book", "300.00", 5), 1);
        when(orderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.cancel(10L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(productRepository).increaseStock(1L, 2);
        verify(productRepository).increaseStock(2L, 1);
    }

    @Test
    void cancel_paidOrder_rejected() {
        Order order = order(10L, user(1L), OrderStatus.PAID);
        when(orderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only PLACED orders can be cancelled. Current status: PAID");

        verify(productRepository, never()).increaseStock(anyLong(), anyInt());
    }

    @Test
    void pay_success_marksOrderPaid() {
        Order order = order(10L, user(1L), OrderStatus.PLACED);
        addToOrder(order, product(1L, "Mouse", "599.00", 10), 2);
        when(orderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.pay(10L, new PaymentRequest(false));

        assertThat(response.status()).isEqualTo(OrderStatus.PAID);
        assertThat(response.payment().status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.payment().amount()).isEqualByComparingTo("1198.00");
        assertThat(response.payment().paidAt()).isNotNull();
    }

    @Test
    void pay_simulatedFailure_keepsOrderPlaced() {
        Order order = order(10L, user(1L), OrderStatus.PLACED);
        addToOrder(order, product(1L, "Mouse", "599.00", 10), 1);
        when(orderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.pay(10L, new PaymentRequest(true));

        assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.payment().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.payment().paidAt()).isNull();
    }

    @Test
    void updateStatus_invalidTransition_rejected() {
        Order order = order(10L, user(1L), OrderStatus.PLACED);
        when(orderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(10L, OrderStatus.SHIPPED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid status change: PLACED -> SHIPPED");
    }

    @Test
    void updateStatus_paidToShipped_allowed() {
        Order order = order(10L, user(1L), OrderStatus.PAID);
        when(orderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.updateStatus(10L, OrderStatus.SHIPPED);

        assertThat(response.status()).isEqualTo(OrderStatus.SHIPPED);
    }
}