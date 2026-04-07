package com.scaler.capstone.cartorder.order.repository;

import com.scaler.capstone.cartorder.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Order> findByIdAndUserId(Long id, String userId);
}
