package com.scaler.capstone.cartorder.order.dto;

import com.scaler.capstone.cartorder.order.model.OrderStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class OrderSummaryResponse {
    Long orderId;
    OrderStatus status;
    BigDecimal totalAmount;
    Integer totalItems;
    Instant createdAt;
}
