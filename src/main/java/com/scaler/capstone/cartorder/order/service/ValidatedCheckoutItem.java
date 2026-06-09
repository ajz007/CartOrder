package com.scaler.capstone.cartorder.order.service;

import com.scaler.capstone.cartorder.product.dto.ProductDetailsResponse;

import java.math.BigDecimal;

record ValidatedCheckoutItem(
        ProductDetailsResponse product,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
