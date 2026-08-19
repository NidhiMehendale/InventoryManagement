package com.pms.service;

import com.pms.dto.request.CartItemRequest;
import com.pms.dto.request.CartItemUpdateRequest;
import com.pms.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(String username);
    CartResponse addToCart(String username, CartItemRequest request);
    CartResponse updateCartItem(String username, Long cartItemId, CartItemUpdateRequest request);
    CartResponse removeCartItem(String username, Long cartItemId);
    void clearCart(String username);
}
