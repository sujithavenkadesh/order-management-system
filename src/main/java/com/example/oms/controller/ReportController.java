package com.example.oms.controller;

import com.example.oms.dto.CategorySalesResponse;
import com.example.oms.dto.CustomerSpendResponse;
import com.example.oms.dto.MonthlyRevenueResponse;
import com.example.oms.dto.TopProductResponse;
import com.example.oms.dto.UnsoldProductResponse;
import com.example.oms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/top-products")
    public List<TopProductResponse> topProducts(@RequestParam(defaultValue = "5") int limit) {
        return reportService.topProducts(limit);
    }

    @GetMapping("/monthly-revenue")
    public List<MonthlyRevenueResponse> monthlyRevenue(@RequestParam(required = false) Integer year) {
        return reportService.monthlyRevenue(year);
    }

    @GetMapping("/customer-spend")
    public List<CustomerSpendResponse> customerSpend(
            @RequestParam(defaultValue = "0") BigDecimal minSpend) {
        return reportService.customerSpend(minSpend);
    }

    @GetMapping("/category-sales")
    public List<CategorySalesResponse> categorySales() {
        return reportService.categorySales();
    }

    @GetMapping("/unsold-products")
    public List<UnsoldProductResponse> unsoldProducts() {
        return reportService.unsoldProducts();
    }
}