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
    @Schema(example = "1")
    Long orderId;
    @Schema(example = "demo@example.com")
    String userId;
    @Schema(example = "1")
    Long cartId;
    @Schema(example = "CONFIRMED")
    OrderStatus status;
    @Schema(example = "2499.00")
    BigDecimal totalAmount;
    @Schema(example = "2117.80")
    BigDecimal subtotal;
    @Schema(example = "0.00")
    BigDecimal discountAmount;
    @Schema(example = "0.00")
    BigDecimal shippingAmount;
    @Schema(example = "381.20")
    BigDecimal taxAmount;
    @Schema(example = "2")
    Integer totalItems;
    List<OrderItemResponse> items;
    ShippingAddressResponse shippingAddress;
    Instant createdAt;
    PaymentSummaryResponse payment;
}
