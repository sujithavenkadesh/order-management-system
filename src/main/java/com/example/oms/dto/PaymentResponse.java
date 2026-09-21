package com.example.oms.dto;

import com.example.oms.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime paidAt) {
}