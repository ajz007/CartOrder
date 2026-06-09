package com.scaler.capstone.cartorder.cart.repository;

import com.scaler.capstone.cartorder.cart.model.Cart;
import com.scaler.capstone.cartorder.cart.model.CartStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndStatus(String userId, CartStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct cart
            from Cart cart
            left join fetch cart.items
            where cart.userId = :userId and cart.status = :status
            """)
    Optional<Cart> findForCheckout(
            @Param("userId") String userId,
            @Param("status") CartStatus status
    );
}
