package com.pms.controller;

import com.pms.dto.response.OrderResponse;
import com.pms.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class OrderController {

    private final OrderService orderService;

    /** Places an order from the current cart. Transactional: fails entirely if any item is out of stock. */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrderHistory(Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrderHistory(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(authentication.getName(), id));
    }
}
