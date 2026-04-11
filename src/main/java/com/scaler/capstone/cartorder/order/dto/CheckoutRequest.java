package com.scaler.capstone.cartorder.order.dto;

import com.scaler.capstone.cartorder.payment.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Checkout request with simulated payment controls")
public class CheckoutRequest {

    @NotNull(message = "paymentMethod is required")
    @Schema(description = "Requested payment method", example = "UPI")
    private PaymentMethod paymentMethod;

    @NotNull(message = "simulateSuccess is required")
    @Schema(description = "Whether the simulated payment should succeed", example = "true")
    private Boolean simulateSuccess;
}
