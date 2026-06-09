package com.scaler.capstone.cartorder.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddCartItemRequest {

    @NotNull(message = "productId is required")
    @Schema(description = "Product id from the catalog", example = "1")
    private Long productId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    @Schema(description = "Number of units to add", example = "1", minimum = "1")
    private Integer quantity;
}
