package com.pms.service.impl;

import com.pms.dto.response.OrderItemResponse;
import com.pms.dto.response.OrderResponse;
import com.pms.entity.*;
import com.pms.enums.OrderStatus;
import com.pms.exception.EmptyCartException;
import com.pms.exception.InsufficientStockException;
import com.pms.exception.ProductDisabledException;
import com.pms.exception.ResourceNotFoundException;
import com.pms.exception.UnauthorizedActionException;
import com.pms.repository.CartRepository;
import com.pms.repository.OrderRepository;
import com.pms.repository.ProductRepository;
import com.pms.repository.UserRepository;
import com.pms.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Places an order from the user's current cart.
     * The entire operation - stock validation, inventory deduction, order
     * creation and cart clearing - runs inside a single DB transaction.
     * If any product has insufficient stock, the whole order fails and
     * nothing is persisted (rollback).
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public OrderResponse placeOrder(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedActionException("User not found: " + username));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new EmptyCartException("Cart is empty. Add items before placing an order."));

        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new EmptyCartException("Cart is empty. Add items before placing an order.");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 1. Validate stock for every item first (fail fast, nothing persisted yet)
        for (CartItem cartItem : cartItems) {
            // Re-fetch product fresh within the transaction to avoid stale reads
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + cartItem.getProduct().getId()));

            if (!product.isEnabled()) {
                throw new ProductDisabledException(
                        "Product '" + product.getName() + "' is disabled and cannot be ordered");
            }

            if (product.getQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + product.getName() + "'. Requested: "
                                + cartItem.getQuantity() + ", Available: " + product.getQuantity());
            }
        }

        // 2. All validations passed -> deduct inventory and build order items
        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + cartItem.getProduct().getId()));

            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .priceAtOrder(product.getPrice())
                    .build());
        }

        // 3. Persist the order
        Order order = Order.builder()
                .user(user)
                .totalAmount(totalAmount)
                .status(OrderStatus.PLACED)
                .build();

        for (OrderItem oi : orderItems) {
            oi.setOrder(order);
        }
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // 4. Clear the cart now that order succeeded
        cart.getCartItems().clear();
        cartRepository.save(cart);

        return toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedActionException("User not found: " + username));
        return orderRepository.findByUserOrderByOrderDateDesc(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String username, Long orderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedActionException("User not found: " + username));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You are not allowed to view this order");
        }

        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(oi -> OrderItemResponse.builder()
                        .productId(oi.getProduct().getId())
                        .productName(oi.getProduct().getName())
                        .quantity(oi.getQuantity())
                        .priceAtOrder(oi.getPriceAtOrder())
                        .subtotal(oi.getPriceAtOrder().multiply(BigDecimal.valueOf(oi.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .items(items)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .orderDate(order.getOrderDate())
                .build();
    }
}
