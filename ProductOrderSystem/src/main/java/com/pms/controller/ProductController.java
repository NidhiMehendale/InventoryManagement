package com.pms.controller;

import com.pms.dto.request.ProductRequest;
import com.pms.dto.request.ProductUpdateRequest;
import com.pms.dto.response.ProductResponse;
import com.pms.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** ADMIN: add a new product */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> addProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(request));
    }

    /** ADMIN: update price and/or quantity of a product */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id,
                                                           @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    /** ADMIN: enable a product */
    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> enableProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.setEnabled(id, true));
    }

    /** ADMIN: disable a product */
    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> disableProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.setEnabled(id, false));
    }

    /** USER & ADMIN: view a single product */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    /**
     * USER & ADMIN: list products.
     * USERs always see only enabled products; ADMINs can pass includeDisabled=true to see all.
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "false") boolean includeDisabled) {
        return ResponseEntity.ok(productService.getAllProducts(!includeDisabled));
    }
}
