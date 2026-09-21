package com.example.oms.config;

import com.example.oms.entity.Category;
import com.example.oms.entity.Product;
import com.example.oms.repository.CategoryRepository;
import com.example.oms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import org.springframework.core.annotation.Order;

@Component
@Order(1)
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        Category gadgets = saveCategory("Gadgets");
        Category books = saveCategory("Books");
        Category fashion = saveCategory("Fashion");

        for (int i = 1; i <= 8; i++) {
            saveProduct("Smartphone " + i, "Smartphone model " + i, 9999 + i * 1500, 20 + i, gadgets);
            saveProduct("Java Book " + i, "Java learning book vol " + i, 299 + i * 50, 100 + i, books);
            saveProduct("T-Shirt " + i, "Cotton t-shirt style " + i, 499 + i * 100, 50 + i, fashion);
        }
        saveProduct("Wireless Mouse", "2.4GHz wireless mouse", 599, 75, gadgets);
    }

    private Category saveCategory(String name) {
        Category c = new Category();
        c.setName(name);
        return categoryRepository.save(c);
    }

    private void saveProduct(String name, String description, int price, int stock, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setPrice(BigDecimal.valueOf(price));
        p.setStockQty(stock);
        p.setCategory(category);
        productRepository.save(p);
    }
}