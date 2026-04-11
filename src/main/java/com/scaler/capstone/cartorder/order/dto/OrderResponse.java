package com.scaler.capstone.cartorder.order.dto;

import com.scaler.capstone.cartorder.order.model.OrderStatus;
import com.scaler.capstone.cartorder.payment.dto.PaymentSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Value
@Builder
@Schema(description = "Detailed order response")
public class OrderResponse {
    @Schema(example = "101")
    Long orderId;
    @Schema(example = "1")
    String userId;
    @Schema(example = "55")
    Long cartId;
    @Schema(example = "CONFIRMED")
    OrderStatus status;
    @Schema(example = "2499.00")
    BigDecimal totalAmount;
    List<OrderItemResponse> items;
    Instant createdAt;
    PaymentSummaryResponse payment;
}
