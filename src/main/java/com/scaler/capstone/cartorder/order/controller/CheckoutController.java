package com.scaler.capstone.cartorder.order.controller;

import com.scaler.capstone.cartorder.order.dto.CheckoutRequest;
import com.scaler.capstone.cartorder.order.dto.OrderResponse;
import com.scaler.capstone.cartorder.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Checkout", description = "Checkout execution endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    @Operation(
            summary = "Checkout active cart",
            description = "Creates an order, simulates payment, updates order status, and marks the cart as checked out.",
            tags = {"Checkout", "Payments"}
    )
    public OrderResponse checkout(Authentication authentication, @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(authentication.getName(), request);
    }
}
