package com.scaler.capstone.cartorder.event;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Value
@Builder
public class OrderCreatedEvent {
    String eventType;
    Long orderId;
    String userId;
    Long cartId;
    BigDecimal totalAmount;
    String status;
    Instant createdAt;
    List<OrderCreatedItemEvent> items;
}
