package com.scaler.capstone.cartorder.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
@Schema(description = "Server-calculated checkout preview using current catalog prices and stock")
public class CheckoutPreviewResponse {
    @Schema(example = "1")
    Long cartId;
    List<CheckoutPreviewItemResponse> items;
    @Schema(example = "2")
    Integer totalItems;
    @Schema(example = "159998.00")
    BigDecimal subtotal;
    @Schema(example = "0.00")
    BigDecimal discountAmount;
    @Schema(example = "0.00")
    BigDecimal shippingAmount;
    @Schema(example = "28799.64")
    BigDecimal taxAmount;
    @Schema(example = "188797.64")
    BigDecimal totalAmount;
}
