package com.example.oms.dto;

import java.math.BigDecimal;

public record CustomerSpendResponse(Long userId, String name, String email,
                                    Long orders, BigDecimal totalSpent) {
}