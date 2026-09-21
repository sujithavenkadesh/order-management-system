package com.example.oms.service;

import com.example.oms.dto.AddCartItemRequest;
import com.example.oms.dto.CartItemResponse;
import com.example.oms.dto.CartResponse;
import com.example.oms.dto.UpdateCartItemRequest;
import com.example.oms.entity.Cart;
import com.example.oms.entity.CartItem;
import com.example.oms.entity.Product;
import com.example.oms.exception.BadRequestException;
import com.example.oms.exception.ResourceNotFoundException;
import com.example.oms.repository.CartRepository;
import com.example.oms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return toResponse(userId, findCart(userId));
    }

    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Cart cart = findCart(userId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + request.productId()));

        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int newQty = (existing == null ? 0 : existing.getQuantity()) + request.quantity();
        checkStock(product, newQty);

        if (existing == null) {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(newQty);
            cart.getItems().add(item);
        } else {
            existing.setQuantity(newQty);
        }

        cartRepository.flush();
        return toResponse(userId, cart);
    }

    @Transactional
    public CartResponse updateQuantity(Long userId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = findCart(userId);
        CartItem item = findItem(cart, itemId);
        checkStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        return toResponse(userId, cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = findCart(userId);
        CartItem item = findItem(cart, itemId);
        cart.getItems().remove(item);
        return toResponse(userId, cart);
    }

    @Transactional
    public void clear(Long userId) {
        findCart(userId).getItems().clear();
    }

    private Cart findCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user: " + userId));
    }

    private CartItem findItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found: " + itemId));
    }

    private void checkStock(Product product, int requestedQty) {
        if (requestedQty > product.getStockQty()) {
            throw new BadRequestException("Only " + product.getStockQty()
                    + " units available for " + product.getName());
        }
    }

    private CartResponse toResponse(Long userId, Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(i -> {
                    BigDecimal price = i.getProduct().getPrice();
                    BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(i.getQuantity()));
                    return new CartItemResponse(i.getId(), i.getProduct().getId(),
                            i.getProduct().getName(), price, i.getQuantity(), lineTotal);
                })
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), userId, items, total);
    }
}