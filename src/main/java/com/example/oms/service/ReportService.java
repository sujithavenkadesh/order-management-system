package com.example.oms.service;

import com.example.oms.dto.CategorySalesResponse;
import com.example.oms.dto.CustomerSpendResponse;
import com.example.oms.dto.MonthlyRevenueResponse;
import com.example.oms.dto.TopProductResponse;
import com.example.oms.dto.UnsoldProductResponse;
import com.example.oms.entity.OrderStatus;
import com.example.oms.exception.BadRequestException;
import com.example.oms.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final List<OrderStatus> REVENUE_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final ReportRepository reportRepository;

    public List<TopProductResponse> topProducts(int limit) {
        if (limit < 1 || limit > 50) {
            throw new BadRequestException("limit must be between 1 and 50");
        }
        return reportRepository.topProducts(REVENUE_STATUSES, PageRequest.of(0, limit));
    }

    public List<MonthlyRevenueResponse> monthlyRevenue(Integer year) {
        int y = (year == null) ? Year.now().getValue() : year;
        return reportRepository.monthlyRevenue(REVENUE_STATUSES, y);
    }

    public List<CustomerSpendResponse> customerSpend(BigDecimal minSpend) {
        return reportRepository.customerSpend(REVENUE_STATUSES, minSpend);
    }

    public List<CategorySalesResponse> categorySales() {
        return reportRepository.categorySales(REVENUE_STATUSES);
    }

    public List<UnsoldProductResponse> unsoldProducts() {
        return reportRepository.unsoldProducts();
    }
}