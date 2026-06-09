package com.scaler.capstone.cartorder.order.service;

import java.math.BigDecimal;
import java.util.List;

record CheckoutPricing(
        List<ValidatedCheckoutItem> items,
        int totalItems,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount
) {
}
