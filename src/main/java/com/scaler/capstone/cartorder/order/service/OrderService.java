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
import com.scaler.capstone.cartorder.order.dto.CheckoutPreviewItemResponse;
import com.scaler.capstone.cartorder.order.dto.CheckoutPreviewResponse;
import com.scaler.capstone.cartorder.order.dto.OrderItemResponse;
import com.scaler.capstone.cartorder.order.dto.OrderResponse;
import com.scaler.capstone.cartorder.order.dto.OrderSummaryResponse;
import com.scaler.capstone.cartorder.order.dto.ShippingAddressRequest;
import com.scaler.capstone.cartorder.order.dto.ShippingAddressResponse;
import com.scaler.capstone.cartorder.order.model.Order;
import com.scaler.capstone.cartorder.order.model.OrderItem;
import com.scaler.capstone.cartorder.order.model.OrderStatus;
import com.scaler.capstone.cartorder.order.model.ShippingAddressSnapshot;
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
    private final CheckoutPricingService checkoutPricingService;

    public OrderService(
            CartRepository cartRepository,
            OrderRepository orderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            OrderEventPublisher orderEventPublisher,
            CheckoutPricingService checkoutPricingService
    ) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderEventPublisher = orderEventPublisher;
        this.checkoutPricingService = checkoutPricingService;
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse previewCheckout(String userId) {
        Cart cart = getCheckoutCart(userId, false);
        CheckoutPricing pricing = checkoutPricingService.calculate(cart);

        return CheckoutPreviewResponse.builder()
                .cartId(cart.getId())
                .items(pricing.items().stream()
                        .map(this::toCheckoutPreviewItemResponse)
                        .toList())
                .totalItems(pricing.totalItems())
                .subtotal(pricing.subtotal())
                .discountAmount(pricing.discountAmount())
                .shippingAmount(pricing.shippingAmount())
                .taxAmount(pricing.taxAmount())
                .totalAmount(pricing.totalAmount())
                .build();
    }

    @Transactional
    public OrderResponse checkout(String userId, CheckoutRequest request) {
        Cart cart = getCheckoutCart(userId, true);
        CheckoutPricing pricing = checkoutPricingService.calculate(cart);

        Order order = new Order();
        order.setUserId(userId);
        order.setCartId(cart.getId());
        order.setStatus(OrderStatus.CREATED);
        order.setSubtotal(pricing.subtotal());
        order.setDiscountAmount(pricing.discountAmount());
        order.setShippingAmount(pricing.shippingAmount());
        order.setTaxAmount(pricing.taxAmount());
        order.setTotalAmount(pricing.totalAmount());
        order.setTotalItems(pricing.totalItems());
        order.setShippingAddress(toShippingAddressSnapshot(request.getShippingAddress()));

        for (ValidatedCheckoutItem validatedItem : pricing.items()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(validatedItem.product().getId());
            orderItem.setProductTitleSnapshot(validatedItem.product().getTitle());
            orderItem.setUnitPrice(validatedItem.unitPrice());
            orderItem.setQuantity(validatedItem.quantity());
            orderItem.setTotalPrice(validatedItem.totalPrice());
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
            cart.setStatus(CartStatus.CHECKED_OUT);
        } else {
            paymentTransaction.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.FAILED);
        }

        paymentTransaction = paymentTransactionRepository.save(paymentTransaction);
        order = orderRepository.save(order);
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            cartRepository.save(cart);
            publishOrderCreatedEvent(order, paymentTransaction);
        }

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

    private Cart getCheckoutCart(String userId, boolean lockForCheckout) {
        Cart cart = (lockForCheckout
                ? cartRepository.findForCheckout(userId, CartStatus.ACTIVE)
                : cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE))
                .orElseThrow(() -> new CartNotFoundException("Active cart not found for user"));
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartCheckoutException("Cannot checkout an empty cart");
        }
        return cart;
    }

    private OrderSummaryResponse toOrderSummaryResponse(Order order) {
        return OrderSummaryResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .totalItems(resolveTotalItems(order))
                .taxAmount(valueOrZero(order.getTaxAmount()))
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
                .subtotal(resolveSubtotal(order))
                .discountAmount(valueOrZero(order.getDiscountAmount()))
                .shippingAmount(valueOrZero(order.getShippingAmount()))
                .taxAmount(valueOrZero(order.getTaxAmount()))
                .totalItems(resolveTotalItems(order))
                .items(items)
                .shippingAddress(toShippingAddressResponse(order.getShippingAddress()))
                .createdAt(order.getCreatedAt())
                .payment(toPaymentSummaryResponse(paymentTransaction))
                .build();
    }

    private CheckoutPreviewItemResponse toCheckoutPreviewItemResponse(ValidatedCheckoutItem item) {
        return CheckoutPreviewItemResponse.builder()
                .productId(item.product().getId())
                .productTitle(item.product().getTitle())
                .unitPrice(item.unitPrice())
                .quantity(item.quantity())
                .totalPrice(item.totalPrice())
                .availableStock(item.product().getStockQuantity())
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

    private ShippingAddressSnapshot toShippingAddressSnapshot(ShippingAddressRequest request) {
        ShippingAddressSnapshot snapshot = new ShippingAddressSnapshot();
        snapshot.setFullName(request.getFullName());
        snapshot.setAddressLine1(request.getAddressLine1());
        snapshot.setAddressLine2(request.getAddressLine2());
        snapshot.setCity(request.getCity());
        snapshot.setState(request.getState());
        snapshot.setPostalCode(request.getPostalCode());
        snapshot.setCountry(request.getCountry());
        snapshot.setPhone(request.getPhone());
        return snapshot;
    }

    private ShippingAddressResponse toShippingAddressResponse(ShippingAddressSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return ShippingAddressResponse.builder()
                .fullName(snapshot.getFullName())
                .addressLine1(snapshot.getAddressLine1())
                .addressLine2(snapshot.getAddressLine2())
                .city(snapshot.getCity())
                .state(snapshot.getState())
                .postalCode(snapshot.getPostalCode())
                .country(snapshot.getCountry())
                .phone(snapshot.getPhone())
                .build();
    }

    private int resolveTotalItems(Order order) {
        if (order.getTotalItems() != null) {
            return order.getTotalItems();
        }
        return order.getItems().stream()
                .map(OrderItem::getQuantity)
                .reduce(0, Integer::sum);
    }

    private BigDecimal resolveSubtotal(Order order) {
        if (order.getSubtotal() != null) {
            return order.getSubtotal();
        }
        return order.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value;
    }

    private void publishOrderCreatedEvent(Order order, PaymentTransaction paymentTransaction) {
        try {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .eventType("order.created")
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .cartId(order.getCartId())
                    .subtotal(order.getSubtotal())
                    .discountAmount(order.getDiscountAmount())
                    .shippingAmount(order.getShippingAmount())
                    .taxAmount(order.getTaxAmount())
                    .totalAmount(order.getTotalAmount())
                    .totalItems(resolveTotalItems(order))
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
