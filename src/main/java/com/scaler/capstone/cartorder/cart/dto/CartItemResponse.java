package com.scaler.capstone.cartorder.cart.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CartItemResponse {
    Long itemId;
    Long productId;
    String productTitle;
    BigDecimal unitPrice;
    Integer quantity;
    BigDecimal totalPrice;
}
