package com.example.oms.dto;

import java.math.BigDecimal;

public record MonthlyRevenueResponse(Integer month, Long orders, BigDecimal revenue) {
}