package com.scaler.capstone.cartorder.order.repository;

import com.scaler.capstone.cartorder.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
