package com.scaler.capstone.cartorder.order.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class OrderItemResponse {
    Long orderItemId;
    Long productId;
    String productTitle;
    BigDecimal unitPrice;
    Integer quantity;
    BigDecimal totalPrice;
}
