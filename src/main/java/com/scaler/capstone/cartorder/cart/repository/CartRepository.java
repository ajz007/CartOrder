package com.scaler.capstone.cartorder.cart.repository;

import com.scaler.capstone.cartorder.cart.model.Cart;
import com.scaler.capstone.cartorder.cart.model.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndStatus(String userId, CartStatus status);
}
