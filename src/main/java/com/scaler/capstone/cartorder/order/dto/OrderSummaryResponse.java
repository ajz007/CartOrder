package com.scaler.capstone.cartorder.order.dto;

import com.scaler.capstone.cartorder.order.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class OrderSummaryResponse {
    @Schema(example = "1")
    Long orderId;
    OrderStatus status;
    @Schema(example = "159998.00")
    BigDecimal totalAmount;
    @Schema(example = "2")
    Integer totalItems;
    @Schema(example = "28799.64")
    BigDecimal taxAmount;
    Instant createdAt;
}
