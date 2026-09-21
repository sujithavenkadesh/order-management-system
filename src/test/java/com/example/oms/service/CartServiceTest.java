package com.example.oms.service;

import com.example.oms.dto.AddCartItemRequest;
import com.example.oms.dto.CartResponse;
import com.example.oms.entity.Cart;
import com.example.oms.entity.Product;
import com.example.oms.exception.BadRequestException;
import com.example.oms.repository.CartRepository;
import com.example.oms.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.example.oms.TestData.addToCart;
import static com.example.oms.TestData.cart;
import static com.example.oms.TestData.product;
import static com.example.oms.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItem_sameProductTwice_mergesIntoOneLine() {
        Product mouse = product(1L, "Mouse", "599.00", 10);
        Cart cart = cart(user(1L));
        addToCart(cart, mouse, 2);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(mouse));

        CartResponse response = cartService.addItem(1L, new AddCartItemRequest(1L, 3));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(5);
        assertThat(response.totalAmount()).isEqualByComparingTo("2995.00");
    }

    @Test
    void addItem_moreThanStock_rejected() {
        Product mouse = product(1L, "Mouse", "599.00", 10);
        Cart cart = cart(user(1L));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(mouse));

        assertThatThrownBy(() -> cartService.addItem(1L, new AddCartItemRequest(1L, 11)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only 10 units available for Mouse");
    }
}