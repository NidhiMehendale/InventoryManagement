package com.pms.service;

import com.pms.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(String username);
    List<OrderResponse> getOrderHistory(String username);
    OrderResponse getOrder(String username, Long orderId);
}
