package com.pms.service;

import com.pms.dto.request.ProductRequest;
import com.pms.dto.request.ProductUpdateRequest;
import com.pms.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse addProduct(ProductRequest request);
    ProductResponse updateProduct(Long productId, ProductUpdateRequest request);
    ProductResponse setEnabled(Long productId, boolean enabled);
    ProductResponse getProduct(Long productId);
    List<ProductResponse> getAllProducts(boolean onlyEnabled);

}
