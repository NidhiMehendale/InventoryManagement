package com.pms.controller;

import com.pms.dto.request.CartItemRequest;
import com.pms.dto.request.CartItemUpdateRequest;
import com.pms.dto.response.CartResponse;
import com.pms.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getCart(authentication.getName()));
    }


    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(Authentication authentication,
                                                    @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addToCart(authentication.getName(), request));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(Authentication authentication,
                                                         @PathVariable Long cartItemId,
                                                         @Valid @RequestBody CartItemUpdateRequest request) {
        return ResponseEntity.ok(cartService.updateCartItem(authentication.getName(), cartItemId, request));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeCartItem(Authentication authentication,
                                                         @PathVariable Long cartItemId) {
        return ResponseEntity.ok(cartService.removeCartItem(authentication.getName(), cartItemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        cartService.clearCart(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
