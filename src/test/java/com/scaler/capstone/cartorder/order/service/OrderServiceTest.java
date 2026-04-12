package com.scaler.capstone.cartorder.order.service;

import com.scaler.capstone.cartorder.cart.model.Cart;
import com.scaler.capstone.cartorder.cart.model.CartItem;
import com.scaler.capstone.cartorder.cart.model.CartStatus;
import com.scaler.capstone.cartorder.cart.repository.CartRepository;
import com.scaler.capstone.cartorder.exception.CartNotFoundException;
import com.scaler.capstone.cartorder.exception.EmptyCartCheckoutException;
import com.scaler.capstone.cartorder.exception.OrderNotFoundException;
import com.scaler.capstone.cartorder.event.OrderCreatedEvent;
import com.scaler.capstone.cartorder.event.OrderEventPublisher;
import com.scaler.capstone.cartorder.order.dto.CheckoutRequest;
import com.scaler.capstone.cartorder.order.dto.OrderResponse;
import com.scaler.capstone.cartorder.order.dto.OrderSummaryResponse;
import com.scaler.capstone.cartorder.order.model.Order;
import com.scaler.capstone.cartorder.order.model.OrderItem;
import com.scaler.capstone.cartorder.order.model.OrderStatus;
import com.scaler.capstone.cartorder.order.repository.OrderRepository;
import com.scaler.capstone.cartorder.payment.model.PaymentMethod;
import com.scaler.capstone.cartorder.payment.model.PaymentStatus;
import com.scaler.capstone.cartorder.payment.model.PaymentTransaction;
import com.scaler.capstone.cartorder.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void checkoutSuccessConfirmsOrderAndPublishesEvent() {
        Cart cart = activeCartWithItems();
        CheckoutRequest request = checkoutRequest(PaymentMethod.UPI, true);
        when(cartRepository.findByUserIdAndStatus("user-1", CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(101L);
                order.setCreatedAt(Instant.parse("2026-04-12T00:00:00Z"));
            }
            long itemId = 1L;
            for (OrderItem item : order.getItems()) {
                if (item.getId() == null) {
                    item.setId(itemId++);
                }
            }
            return order;
        });
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(501L);
            }
            return payment;
        });
        when(cartRepository.save(cart)).thenReturn(cart);

        OrderResponse response = orderService.checkout("user-1", request);

        assertThat(response.getOrderId()).isEqualTo(101L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.getPayment().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventPublisher).publishOrderCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(eventCaptor.getValue().getPaymentMethod()).isEqualTo("UPI");
    }

    @Test
    void checkoutPaymentFailureMarksOrderFailed() {
        Cart cart = activeCartWithItems();
        when(cartRepository.findByUserIdAndStatus("user-1", CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(101L);
                order.setCreatedAt(Instant.parse("2026-04-12T00:00:00Z"));
            }
            return order;
        });
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(501L);
            }
            return payment;
        });
        when(cartRepository.save(cart)).thenReturn(cart);

        OrderResponse response = orderService.checkout("user-1", checkoutRequest(PaymentMethod.CARD, false));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(response.getPayment().getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void checkoutRejectsEmptyCart() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUserId("user-1");
        cart.setStatus(CartStatus.ACTIVE);
        when(cartRepository.findByUserIdAndStatus("user-1", CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.checkout("user-1", checkoutRequest(PaymentMethod.UPI, true)))
                .isInstanceOf(EmptyCartCheckoutException.class)
                .hasMessage("Cannot checkout an empty cart");
    }

    @Test
    void checkoutRejectsMissingCart() {
        when(cartRepository.findByUserIdAndStatus("user-1", CartStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.checkout("user-1", checkoutRequest(PaymentMethod.UPI, true)))
                .isInstanceOf(CartNotFoundException.class)
                .hasMessage("Active cart not found for user");
    }

    @Test
    void getOrdersReturnsHistory() {
        Order order = orderWithItems(101L, OrderStatus.CONFIRMED);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(order));

        List<OrderSummaryResponse> responses = orderService.getOrders("user-1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getOrderId()).isEqualTo(101L);
        assertThat(responses.get(0).getTotalItems()).isEqualTo(2);
    }

    @Test
    void getOrderByIdReturnsResponse() {
        Order order = orderWithItems(101L, OrderStatus.CONFIRMED);
        PaymentTransaction payment = paymentTransaction(101L, PaymentStatus.SUCCESS, PaymentMethod.UPI);
        when(orderRepository.findByIdAndUserId(101L, "user-1")).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByOrderId(101L)).thenReturn(Optional.of(payment));

        OrderResponse response = orderService.getOrder("user-1", 101L);

        assertThat(response.getOrderId()).isEqualTo(101L);
        assertThat(response.getPayment().getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
    }

    @Test
    void getOrderByIdThrowsWhenMissing() {
        when(orderRepository.findByIdAndUserId(999L, "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder("user-1", 999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order not found: 999");
    }

    private Cart activeCartWithItems() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUserId("user-1");
        cart.setStatus(CartStatus.ACTIVE);

        CartItem item = new CartItem();
        item.setId(11L);
        item.setCart(cart);
        item.setProductId(1L);
        item.setProductTitleSnapshot("Phone");
        item.setUnitPrice(BigDecimal.valueOf(500));
        item.setQuantity(2);
        item.setTotalPrice(BigDecimal.valueOf(1000));
        cart.getItems().add(item);
        return cart;
    }

    private CheckoutRequest checkoutRequest(PaymentMethod method, boolean simulateSuccess) {
        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(method);
        request.setSimulateSuccess(simulateSuccess);
        return request;
    }

    private Order orderWithItems(Long id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId("user-1");
        order.setCartId(1L);
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.valueOf(1000));
        order.setCreatedAt(Instant.parse("2026-04-12T00:00:00Z"));

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProductId(1L);
        item.setProductTitleSnapshot("Phone");
        item.setUnitPrice(BigDecimal.valueOf(500));
        item.setQuantity(2);
        item.setTotalPrice(BigDecimal.valueOf(1000));
        order.getItems().add(item);
        return order;
    }

    private PaymentTransaction paymentTransaction(Long orderId, PaymentStatus status, PaymentMethod method) {
        PaymentTransaction payment = new PaymentTransaction();
        payment.setId(501L);
        payment.setOrderId(orderId);
        payment.setUserId("user-1");
        payment.setAmount(BigDecimal.valueOf(1000));
        payment.setStatus(status);
        payment.setPaymentMethod(method);
        payment.setGatewayReference("SIM-123");
        return payment;
    }
}
