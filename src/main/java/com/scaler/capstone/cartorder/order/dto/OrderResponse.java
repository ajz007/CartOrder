package com.scaler.capstone.cartorder.order.dto;

import com.scaler.capstone.cartorder.order.model.OrderStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Value
@Builder
public class OrderResponse {
    Long orderId;
    String userId;
    Long cartId;
    OrderStatus status;
    BigDecimal totalAmount;
    List<OrderItemResponse> items;
    Instant createdAt;
}
