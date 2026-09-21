package com.example.oms.service;

import com.example.oms.dto.PageResponse;
import com.example.oms.dto.ProductRequest;
import com.example.oms.dto.ProductResponse;
import com.example.oms.entity.Category;
import com.example.oms.entity.Product;
import com.example.oms.exception.BadRequestException;
import com.example.oms.exception.ResourceNotFoundException;
import com.example.oms.repository.CategoryRepository;
import com.example.oms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "price", "stockQty");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String keyword, Long categoryId,
                                                BigDecimal minPrice, BigDecimal maxPrice,
                                                Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by: " + order.getProperty());
            }
        });

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<ProductResponse> page = productRepository
                .search(kw, categoryId, minPrice, maxPrice, pageable)
                .map(this::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findOrThrow(id);
        apply(product, request);
        return toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(findOrThrow(id));
    }

    private void apply(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + request.categoryId()));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQty(request.stockQty());
        product.setCategory(category);
    }

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStockQty(), p.getCategory().getId(), p.getCategory().getName());
    }
}