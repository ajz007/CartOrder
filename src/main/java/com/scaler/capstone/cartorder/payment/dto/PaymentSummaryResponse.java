package com.scaler.capstone.cartorder.payment.dto;

import com.scaler.capstone.cartorder.payment.model.PaymentMethod;
import com.scaler.capstone.cartorder.payment.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
@Schema(description = "Payment summary associated with an order")
public class PaymentSummaryResponse {
    @Schema(example = "501")
    Long paymentId;
    @Schema(example = "SUCCESS")
    PaymentStatus status;
    @Schema(example = "UPI")
    PaymentMethod paymentMethod;
    @Schema(example = "2499.00")
    BigDecimal amount;
    @Schema(example = "SIM-123e4567-e89b-12d3-a456-426614174000")
    String gatewayReference;
}
