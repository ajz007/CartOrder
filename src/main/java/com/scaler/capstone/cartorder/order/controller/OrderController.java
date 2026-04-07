package com.scaler.capstone.cartorder.order.controller;

import com.scaler.capstone.cartorder.order.dto.OrderResponse;
import com.scaler.capstone.cartorder.order.dto.OrderSummaryResponse;
import com.scaler.capstone.cartorder.order.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderSummaryResponse> getOrders(Authentication authentication) {
        return orderService.getOrders(authentication.getName());
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(Authentication authentication, @PathVariable Long orderId) {
        return orderService.getOrder(authentication.getName(), orderId);
    }
}
