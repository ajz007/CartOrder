package com.scaler.capstone.cartorder.cart.dto;

import com.scaler.capstone.cartorder.cart.model.CartStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CartResponse {
    @Schema(example = "1")
    Long cartId;
    String userId;
    CartStatus status;
    List<CartItemResponse> items;
    @Schema(example = "79999.00")
    BigDecimal totalAmount;
    @Schema(example = "2")
    Integer totalItems;
    boolean empty;
}
