package com.scaler.capstone.cartorder.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class OrderItemResponse {
    @Schema(example = "1")
    Long orderItemId;
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
