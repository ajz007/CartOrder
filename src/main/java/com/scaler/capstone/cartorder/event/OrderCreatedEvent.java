package com.scaler.capstone.cartorder.event;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Value
@Builder
@Jacksonized
public class OrderCreatedEvent {
    String eventType;
    Long orderId;
    String userId;
    Long cartId;
    BigDecimal subtotal;
    BigDecimal discountAmount;
    BigDecimal shippingAmount;
    BigDecimal taxAmount;
    BigDecimal totalAmount;
    Integer totalItems;
    String status;
    String paymentStatus;
    String paymentMethod;
    Instant createdAt;
    List<OrderCreatedItemEvent> items;
}
