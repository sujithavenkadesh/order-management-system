package com.example.oms.repository;

import com.example.oms.dto.CategorySalesResponse;
import com.example.oms.dto.CustomerSpendResponse;
import com.example.oms.dto.MonthlyRevenueResponse;
import com.example.oms.dto.TopProductResponse;
import com.example.oms.dto.UnsoldProductResponse;
import com.example.oms.entity.Order;
import com.example.oms.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface ReportRepository extends Repository<Order, Long> {

    @Query("""
            SELECT new com.example.oms.dto.TopProductResponse(
                p.id, p.name, SUM(oi.quantity), SUM(oi.quantity * oi.priceAtPurchase))
            FROM OrderItem oi
            JOIN oi.product p
            JOIN oi.order o
            WHERE o.status IN :statuses
            GROUP BY p.id, p.name
            ORDER BY SUM(oi.quantity) DESC, p.name ASC
            """)
    List<TopProductResponse> topProducts(@Param("statuses") Collection<OrderStatus> statuses,
                                         Pageable pageable);

    @Query("""
            SELECT new com.example.oms.dto.MonthlyRevenueResponse(
                EXTRACT(MONTH FROM o.createdAt), COUNT(o), SUM(o.totalAmount))
            FROM Order o
            WHERE o.status IN :statuses AND EXTRACT(YEAR FROM o.createdAt) = :year
            GROUP BY EXTRACT(MONTH FROM o.createdAt)
            ORDER BY EXTRACT(MONTH FROM o.createdAt)
            """)
    List<MonthlyRevenueResponse> monthlyRevenue(@Param("statuses") Collection<OrderStatus> statuses,
                                                @Param("year") int year);

    @Query("""
            SELECT new com.example.oms.dto.CustomerSpendResponse(
                u.id, u.name, u.email, COUNT(o), SUM(o.totalAmount))
            FROM Order o
            JOIN o.user u
            WHERE o.status IN :statuses
            GROUP BY u.id, u.name, u.email
            HAVING SUM(o.totalAmount) >= :minSpend
            ORDER BY SUM(o.totalAmount) DESC
            """)
    List<CustomerSpendResponse> customerSpend(@Param("statuses") Collection<OrderStatus> statuses,
                                              @Param("minSpend") BigDecimal minSpend);

    @Query("""
            SELECT new com.example.oms.dto.CategorySalesResponse(
                c.id, c.name, SUM(oi.quantity), SUM(oi.quantity * oi.priceAtPurchase))
            FROM OrderItem oi
            JOIN oi.product p
            JOIN p.category c
            JOIN oi.order o
            WHERE o.status IN :statuses
            GROUP BY c.id, c.name
            ORDER BY SUM(oi.quantity * oi.priceAtPurchase) DESC
            """)
    List<CategorySalesResponse> categorySales(@Param("statuses") Collection<OrderStatus> statuses);

    @Query("""
            SELECT new com.example.oms.dto.UnsoldProductResponse(p.id, p.name, p.stockQty)
            FROM Product p
            LEFT JOIN OrderItem oi ON oi.product = p
            WHERE oi.id IS NULL
            ORDER BY p.id
            """)
    List<UnsoldProductResponse> unsoldProducts();
}