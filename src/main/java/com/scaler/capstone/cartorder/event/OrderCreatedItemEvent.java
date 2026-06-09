package com.scaler.capstone.cartorder.event;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Value
@Builder
@Jacksonized
public class OrderCreatedItemEvent {
    Long productId;
    String productTitle;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal totalPrice;
}
