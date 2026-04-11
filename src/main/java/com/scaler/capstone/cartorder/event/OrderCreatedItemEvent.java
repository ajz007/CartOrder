package com.scaler.capstone.cartorder.event;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class OrderCreatedItemEvent {
    Long productId;
    String productTitle;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal totalPrice;
}
