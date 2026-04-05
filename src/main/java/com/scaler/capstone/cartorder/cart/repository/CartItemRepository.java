package com.scaler.capstone.cartorder.cart.repository;

import com.scaler.capstone.cartorder.cart.model.Cart;
import com.scaler.capstone.cartorder.cart.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCart(Cart cart);

    Optional<CartItem> findByIdAndCart(Long id, Cart cart);

    Optional<CartItem> findByCartAndProductId(Cart cart, Long productId);

    void deleteByCart(Cart cart);
}
