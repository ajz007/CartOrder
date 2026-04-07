package com.scaler.capstone.cartorder.order.controller;

import com.scaler.capstone.cartorder.order.dto.OrderResponse;
import com.scaler.capstone.cartorder.order.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse checkout(Authentication authentication) {
        return orderService.checkout(authentication.getName());
    }
}
