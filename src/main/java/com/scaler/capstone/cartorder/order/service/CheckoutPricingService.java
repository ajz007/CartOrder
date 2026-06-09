package com.scaler.capstone.cartorder.order.service;

import com.scaler.capstone.cartorder.cart.model.Cart;
import com.scaler.capstone.cartorder.cart.model.CartItem;
import com.scaler.capstone.cartorder.exception.ProductUnavailableException;
import com.scaler.capstone.cartorder.product.client.ProductClient;
import com.scaler.capstone.cartorder.product.dto.ProductDetailsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckoutPricingService {

    private static final int MONEY_SCALE = 2;

    private final ProductClient productClient;
    private final BigDecimal taxRate;
    private final BigDecimal shippingFee;

    public CheckoutPricingService(
            ProductClient productClient,
            @Value("${app.checkout.tax-rate}") BigDecimal taxRate,
            @Value("${app.checkout.shipping-fee}") BigDecimal shippingFee
    ) {
        if (taxRate.signum() < 0) {
            throw new IllegalArgumentException("app.checkout.tax-rate cannot be negative");
        }
        if (shippingFee.signum() < 0) {
            throw new IllegalArgumentException("app.checkout.shipping-fee cannot be negative");
        }
        this.productClient = productClient;
        this.taxRate = taxRate;
        this.shippingFee = money(shippingFee);
    }

    CheckoutPricing calculate(Cart cart) {
        List<ValidatedCheckoutItem> validatedItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem cartItem : cart.getItems()) {
            ProductDetailsResponse product = productClient.getProductById(cartItem.getProductId());
            validateProduct(product, cartItem);

            BigDecimal unitPrice = money(product.getPrice());
            BigDecimal itemTotal = money(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            validatedItems.add(new ValidatedCheckoutItem(product, cartItem.getQuantity(), unitPrice, itemTotal));
            subtotal = subtotal.add(itemTotal);
            totalItems += cartItem.getQuantity();
        }

        subtotal = money(subtotal);
        BigDecimal discountAmount = money(BigDecimal.ZERO);
        BigDecimal taxableAmount = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = money(taxableAmount.multiply(taxRate));
        BigDecimal totalAmount = money(taxableAmount.add(shippingFee).add(taxAmount));

        return new CheckoutPricing(
                List.copyOf(validatedItems),
                totalItems,
                subtotal,
                discountAmount,
                shippingFee,
                taxAmount,
                totalAmount
        );
    }

    private void validateProduct(ProductDetailsResponse product, CartItem cartItem) {
        if (product.getTitle() == null || product.getTitle().isBlank()) {
            throw new ProductUnavailableException("Product has no current title: " + cartItem.getProductId());
        }
        if (product.getPrice() == null) {
            throw new ProductUnavailableException("Product has no current price: " + cartItem.getProductId());
        }
        if (product.getStockQuantity() == null || product.getStockQuantity() < cartItem.getQuantity()) {
            int availableStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
            throw new ProductUnavailableException(
                    "Insufficient stock for product %d: requested %d, available %d"
                            .formatted(cartItem.getProductId(), cartItem.getQuantity(), availableStock)
            );
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
