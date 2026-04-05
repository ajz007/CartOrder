package com.scaler.capstone.cartorder.cart.dto;

import com.scaler.capstone.cartorder.cart.model.CartStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class CartResponse {
    Long cartId;
    String userId;
    CartStatus status;
    List<CartItemResponse> items;
    BigDecimal totalAmount;
    Integer totalItems;
    boolean empty;
}
