package com.scaler.capstone.cartorder.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class CartItemResponse {
    @Schema(example = "1")
    Long itemId;
    @Schema(example = "1")
    Long productId;
    String productTitle;
    @Schema(example = "79999.00")
    BigDecimal unitPrice;
    @Schema(example = "2")
    Integer quantity;
    @Schema(example = "159998.00")
    BigDecimal totalPrice;
}
