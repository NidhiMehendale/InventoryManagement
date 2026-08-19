package com.pms.service.impl;

import com.pms.dto.request.ProductRequest;
import com.pms.dto.request.ProductUpdateRequest;
import com.pms.dto.response.ProductResponse;
import com.pms.entity.Product;
import com.pms.exception.ResourceNotFoundException;
import com.pms.repository.ProductRepository;
import com.pms.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse addProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .enabled(true)
                .build();
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = findProductOrThrow(productId);

        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getQuantity() != null) {
            product.setQuantity(request.getQuantity());
        }
        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse setEnabled(Long productId, boolean enabled) {
        Product product = findProductOrThrow(productId);
        product.setEnabled(enabled);
        return toResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse getProduct(Long productId) {
        return toResponse(findProductOrThrow(productId));
    }

    @Override
    public List<ProductResponse> getAllProducts(boolean onlyEnabled) {
        List<Product> products = onlyEnabled ? productRepository.findByEnabledTrue() : productRepository.findAll();
        return products.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .enabled(product.isEnabled())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
