package com.scaler.capstone.cartorder.order.service;

import com.scaler.capstone.cartorder.cart.model.Cart;
import com.scaler.capstone.cartorder.cart.model.CartItem;
import com.scaler.capstone.cartorder.cart.model.CartStatus;
import com.scaler.capstone.cartorder.cart.repository.CartRepository;
import com.scaler.capstone.cartorder.exception.CartNotFoundException;
import com.scaler.capstone.cartorder.exception.EmptyCartCheckoutException;
import com.scaler.capstone.cartorder.exception.OrderNotFoundException;
import com.scaler.capstone.cartorder.order.dto.OrderItemResponse;
import com.scaler.capstone.cartorder.order.dto.OrderResponse;
import com.scaler.capstone.cartorder.order.dto.OrderSummaryResponse;
import com.scaler.capstone.cartorder.order.model.Order;
import com.scaler.capstone.cartorder.order.model.OrderItem;
import com.scaler.capstone.cartorder.order.model.OrderStatus;
import com.scaler.capstone.cartorder.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    public OrderService(CartRepository cartRepository, OrderRepository orderRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public OrderResponse checkout(String userId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException("Active cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartCheckoutException("Cannot checkout an empty cart");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setCartId(cart.getId());
        order.setStatus(OrderStatus.CREATED);

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> toOrderItem(order, cartItem))
                .toList();
        order.setItems(orderItems);
        order.setTotalAmount(orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        return toOrderResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toOrderSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return toOrderResponse(order);
    }

    private OrderItem toOrderItem(Order order, CartItem cartItem) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductId(cartItem.getProductId());
        orderItem.setProductTitleSnapshot(cartItem.getProductTitleSnapshot());
        orderItem.setUnitPrice(cartItem.getUnitPrice());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setTotalPrice(cartItem.getTotalPrice());
        return orderItem;
    }

    private OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .cartId(order.getCartId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream().map(this::toOrderItemResponse).toList())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .orderItemId(orderItem.getId())
                .productId(orderItem.getProductId())
                .productTitle(orderItem.getProductTitleSnapshot())
                .unitPrice(orderItem.getUnitPrice())
                .quantity(orderItem.getQuantity())
                .totalPrice(orderItem.getTotalPrice())
                .build();
    }

    private OrderSummaryResponse toOrderSummaryResponse(Order order) {
        int totalItems = order.getItems().stream()
                .map(OrderItem::getQuantity)
                .reduce(0, Integer::sum);

        return OrderSummaryResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .totalItems(totalItems)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
