package com.pms.service.impl;

import com.pms.dto.request.CartItemRequest;
import com.pms.dto.request.CartItemUpdateRequest;
import com.pms.dto.response.CartItemResponse;
import com.pms.dto.response.CartResponse;
import com.pms.entity.Cart;
import com.pms.entity.CartItem;
import com.pms.entity.Product;
import com.pms.entity.User;
import com.pms.exception.InsufficientStockException;
import com.pms.exception.ProductDisabledException;
import com.pms.exception.ResourceNotFoundException;
import com.pms.exception.UnauthorizedActionException;
import com.pms.repository.CartItemRepository;
import com.pms.repository.CartRepository;
import com.pms.repository.ProductRepository;
import com.pms.repository.UserRepository;
import com.pms.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String username) {
        Cart cart = getOrCreateCart(username);
        return toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(String username, CartItemRequest request) {
        Cart cart = getOrCreateCart(username);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!product.isEnabled()) {
            throw new ProductDisabledException("Product '" + product.getName() + "' is currently disabled");
        }

        CartItem existingItem = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);
        int desiredQuantity = request.getQuantity() + (existingItem != null ? existingItem.getQuantity() : 0);

        if (desiredQuantity > product.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + product.getName() + "'. Available: " + product.getQuantity());
        }

        if (existingItem != null) {
            existingItem.setQuantity(desiredQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        return toResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(String username, Long cartItemId, CartItemUpdateRequest request) {
        Cart cart = getOrCreateCart(username);
        CartItem item = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        Product product = item.getProduct();
        if (!product.isEnabled()) {
            throw new ProductDisabledException("Product '" + product.getName() + "' is currently disabled");
        }
        if (request.getQuantity() > product.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + product.getName() + "'. Available: " + product.getQuantity());
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return toResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(String username, Long cartItemId) {
        Cart cart = getOrCreateCart(username);
        CartItem item = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));
        cartItemRepository.delete(item);
        return toResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Override
    @Transactional
    public void clearCart(String username) {
        Cart cart = getOrCreateCart(username);
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedActionException("User not found: " + username));
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getCartItems().stream()
                .map(ci -> CartItemResponse.builder()
                        .id(ci.getId())
                        .productId(ci.getProduct().getId())
                        .productName(ci.getProduct().getName())
                        .unitPrice(ci.getProduct().getPrice())
                        .quantity(ci.getQuantity())
                        .subtotal(ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .totalAmount(total)
                .build();
    }
}
