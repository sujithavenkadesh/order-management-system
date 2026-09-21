package com.example.oms.dto;

public record UnsoldProductResponse(Long productId, String productName, Integer stockQty) {
}