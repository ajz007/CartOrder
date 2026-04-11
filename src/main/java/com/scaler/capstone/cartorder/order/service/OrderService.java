package com.scaler.capstone.cartorder.order.service;

import com.scaler.capstone.cartorder.cart.model.Cart;
import com.scaler.capstone.cartorder.cart.model.CartItem;
import com.scaler.capstone.cartorder.cart.model.CartStatus;
import com.scaler.capstone.cartorder.cart.repository.CartRepository;
import com.scaler.capstone.cartorder.exception.CartNotFoundException;
import com.scaler.capstone.cartorder.exception.EmptyCartCheckoutException;
import com.scaler.capstone.cartorder.exception.OrderNotFoundException;
import com.scaler.capstone.cartorder.event.OrderCreatedEvent;
import com.scaler.capstone.cartorder.event.OrderCreatedItemEvent;
import com.scaler.capstone.cartorder.event.OrderEventPublisher;
import com.scaler.capstone.cartorder.order.dto.CheckoutRequest;
import com.scaler.capstone.cartorder.order.dto.OrderItemResponse;
import com.scaler.capstone.cartorder.order.dto.OrderResponse;
import com.scaler.capstone.cartorder.order.dto.OrderSummaryResponse;
import com.scaler.capstone.cartorder.order.model.Order;
import com.scaler.capstone.cartorder.order.model.OrderItem;
import com.scaler.capstone.cartorder.order.model.OrderStatus;
import com.scaler.capstone.cartorder.order.repository.OrderRepository;
import com.scaler.capstone.cartorder.payment.dto.PaymentSummaryResponse;
import com.scaler.capstone.cartorder.payment.model.PaymentStatus;
import com.scaler.capstone.cartorder.payment.model.PaymentTransaction;
import com.scaler.capstone.cartorder.payment.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(
            CartRepository cartRepository,
            OrderRepository orderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            OrderEventPublisher orderEventPublisher
    ) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public OrderResponse checkout(String userId, CheckoutRequest request) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartNotFoundException("Active cart not found for user"));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartCheckoutException("Cannot checkout an empty cart");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setCartId(cart.getId());
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(calculateCartTotal(cart));

        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductTitleSnapshot(cartItem.getProductTitleSnapshot());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(cartItem.getTotalPrice());
            order.getItems().add(orderItem);
        }

        order = orderRepository.save(order);

        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setOrderId(order.getId());
        paymentTransaction.setUserId(userId);
        paymentTransaction.setAmount(order.getTotalAmount());
        paymentTransaction.setPaymentMethod(request.getPaymentMethod());
        paymentTransaction.setStatus(PaymentStatus.INITIATED);
        paymentTransaction.setGatewayReference("SIM-" + UUID.randomUUID());
        paymentTransaction = paymentTransactionRepository.save(paymentTransaction);

        if (Boolean.TRUE.equals(request.getSimulateSuccess())) {
            paymentTransaction.setStatus(PaymentStatus.SUCCESS);
            order.setStatus(OrderStatus.CONFIRMED);
        } else {
            paymentTransaction.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.FAILED);
        }

        cart.setStatus(CartStatus.CHECKED_OUT);

        paymentTransaction = paymentTransactionRepository.save(paymentTransaction);
        order = orderRepository.save(order);
        cartRepository.save(cart);

        publishOrderCreatedEvent(order, paymentTransaction);

        return toOrderResponse(order, paymentTransaction);
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
        PaymentTransaction paymentTransaction = paymentTransactionRepository.findByOrderId(order.getId())
                .orElse(null);
        return toOrderResponse(order, paymentTransaction);
    }

    private BigDecimal calculateCartTotal(Cart cart) {
        return cart.getItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private OrderResponse toOrderResponse(Order order, PaymentTransaction paymentTransaction) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .cartId(order.getCartId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(items)
                .createdAt(order.getCreatedAt())
                .payment(toPaymentSummaryResponse(paymentTransaction))
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

    private PaymentSummaryResponse toPaymentSummaryResponse(PaymentTransaction paymentTransaction) {
        if (paymentTransaction == null) {
            return null;
        }

        return PaymentSummaryResponse.builder()
                .paymentId(paymentTransaction.getId())
                .status(paymentTransaction.getStatus())
                .paymentMethod(paymentTransaction.getPaymentMethod())
                .amount(paymentTransaction.getAmount())
                .gatewayReference(paymentTransaction.getGatewayReference())
                .build();
    }

    private void publishOrderCreatedEvent(Order order, PaymentTransaction paymentTransaction) {
        try {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .eventType("order.created")
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .cartId(order.getCartId())
                    .totalAmount(order.getTotalAmount())
                    .status(order.getStatus().name())
                    .paymentStatus(paymentTransaction.getStatus().name())
                    .paymentMethod(paymentTransaction.getPaymentMethod().name())
                    .createdAt(order.getCreatedAt())
                    .items(order.getItems().stream()
                            .map(this::toOrderCreatedItemEvent)
                            .toList())
                    .build();
            orderEventPublisher.publishOrderCreated(event);
        } catch (Exception ex) {
            LOGGER.error("Failed to trigger order.created event for order {}", order.getId(), ex);
        }
    }

    private OrderCreatedItemEvent toOrderCreatedItemEvent(OrderItem orderItem) {
        return OrderCreatedItemEvent.builder()
                .productId(orderItem.getProductId())
                .productTitle(orderItem.getProductTitleSnapshot())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .totalPrice(orderItem.getTotalPrice())
                .build();
    }
}
