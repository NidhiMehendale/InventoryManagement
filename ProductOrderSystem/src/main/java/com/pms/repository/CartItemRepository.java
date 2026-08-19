package com.pms.repository;

import com.pms.entity.Cart;
import com.pms.entity.CartItem;
import com.pms.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    Optional<CartItem> findByIdAndCart(Long id, Cart cart);
}
