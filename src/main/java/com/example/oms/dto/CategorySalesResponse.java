package com.example.oms.dto;

import java.math.BigDecimal;

public record CategorySalesResponse(Long categoryId, String categoryName,
                                    Long unitsSold, BigDecimal revenue) {
}