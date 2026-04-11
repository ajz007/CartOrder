package com.scaler.capstone.cartorder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.capstone.cartorder.cart.controller.CartController;
import com.scaler.capstone.cartorder.cart.dto.AddCartItemRequest;
import com.scaler.capstone.cartorder.cart.dto.CartItemResponse;
import com.scaler.capstone.cartorder.cart.dto.CartResponse;
import com.scaler.capstone.cartorder.cart.dto.UpdateCartItemRequest;
import com.scaler.capstone.cartorder.cart.model.CartStatus;
import com.scaler.capstone.cartorder.cart.service.CartService;
import com.scaler.capstone.cartorder.exception.CartItemNotFoundException;
import com.scaler.capstone.cartorder.exception.EmptyCartCheckoutException;
import com.scaler.capstone.cartorder.order.controller.CheckoutController;
import com.scaler.capstone.cartorder.order.controller.OrderController;
import com.scaler.capstone.cartorder.order.dto.CheckoutRequest;
import com.scaler.capstone.cartorder.order.dto.OrderItemResponse;
import com.scaler.capstone.cartorder.order.dto.OrderResponse;
import com.scaler.capstone.cartorder.order.dto.OrderSummaryResponse;
import com.scaler.capstone.cartorder.order.model.OrderStatus;
import com.scaler.capstone.cartorder.order.service.OrderService;
import com.scaler.capstone.cartorder.payment.dto.PaymentSummaryResponse;
import com.scaler.capstone.cartorder.payment.model.PaymentMethod;
import com.scaler.capstone.cartorder.payment.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CartOrderControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new CartController(cartService),
                        new CheckoutController(orderService),
                        new OrderController(orderService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addItemReturnsCartResponse() throws Exception {
        when(cartService.addItem(any(), any(AddCartItemRequest.class))).thenReturn(cartResponse());

        mockMvc.perform(post("/cart/items")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.items[0].productTitle").value("Phone"));
    }

    @Test
    void getCartReturnsCartResponse() throws Exception {
        when(cartService.getCurrentCart(any())).thenReturn(cartResponse());

        mockMvc.perform(get("/cart").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void updateItemReturnsCartResponse() throws Exception {
        when(cartService.updateItem(any(), anyLong(), any(UpdateCartItemRequest.class))).thenReturn(cartResponse());

        mockMvc.perform(put("/cart/items/11")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void removeItemReturnsCartResponse() throws Exception {
        when(cartService.removeItem(any(), anyLong())).thenReturn(cartResponse());

        mockMvc.perform(delete("/cart/items/11").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1));
    }

    @Test
    void clearCartReturnsCartResponse() throws Exception {
        when(cartService.clearCart(any())).thenReturn(cartResponse());

        mockMvc.perform(delete("/cart").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(false));
    }

    @Test
    void checkoutReturnsOrderResponse() throws Exception {
        when(orderService.checkout(any(), any(CheckoutRequest.class))).thenReturn(orderResponse(OrderStatus.CONFIRMED, PaymentStatus.SUCCESS));

        mockMvc.perform(post("/checkout")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(101))
                .andExpect(jsonPath("$.payment.status").value("SUCCESS"));
    }

    @Test
    void checkoutReturnsBadRequestForEmptyCart() throws Exception {
        doThrow(new EmptyCartCheckoutException("Cannot checkout an empty cart"))
                .when(orderService).checkout(any(), any(CheckoutRequest.class));

        mockMvc.perform(post("/checkout")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest(true))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cannot checkout an empty cart"));
    }

    @Test
    void getOrdersReturnsOrderHistory() throws Exception {
        when(orderService.getOrders(any())).thenReturn(List.of(orderSummaryResponse()));

        mockMvc.perform(get("/orders").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(101))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    void getOrderReturnsOrderDetails() throws Exception {
        when(orderService.getOrder(any(), anyLong())).thenReturn(orderResponse(OrderStatus.CONFIRMED, PaymentStatus.SUCCESS));

        mockMvc.perform(get("/orders/101").principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(101))
                .andExpect(jsonPath("$.payment.paymentMethod").value("UPI"));
    }

    @Test
    void cartNotFoundReturns404() throws Exception {
        when(cartService.getCurrentCart(any())).thenThrow(new CartItemNotFoundException("Cart item not found: 11"));

        mockMvc.perform(get("/cart").principal(auth()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Cart item not found: 11"));
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken("user@example.com", "n/a", List.of());
    }

    private AddCartItemRequest addRequest() {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(1L);
        request.setQuantity(2);
        return request;
    }

    private UpdateCartItemRequest updateRequest() {
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(2);
        return request;
    }

    private CheckoutRequest checkoutRequest(boolean simulateSuccess) {
        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.UPI);
        request.setSimulateSuccess(simulateSuccess);
        return request;
    }

    private CartResponse cartResponse() {
        return CartResponse.builder()
                .cartId(1L)
                .userId("user@example.com")
                .status(CartStatus.ACTIVE)
                .items(List.of(CartItemResponse.builder()
                        .itemId(11L)
                        .productId(1L)
                        .productTitle("Phone")
                        .unitPrice(BigDecimal.valueOf(500))
                        .quantity(2)
                        .totalPrice(BigDecimal.valueOf(1000))
                        .build()))
                .totalAmount(BigDecimal.valueOf(1000))
                .totalItems(2)
                .empty(false)
                .build();
    }

    private OrderResponse orderResponse(OrderStatus orderStatus, PaymentStatus paymentStatus) {
        return OrderResponse.builder()
                .orderId(101L)
                .userId("user@example.com")
                .cartId(1L)
                .status(orderStatus)
                .totalAmount(BigDecimal.valueOf(1000))
                .items(List.of(OrderItemResponse.builder()
                        .orderItemId(1L)
                        .productId(1L)
                        .productTitle("Phone")
                        .unitPrice(BigDecimal.valueOf(500))
                        .quantity(2)
                        .totalPrice(BigDecimal.valueOf(1000))
                        .build()))
                .createdAt(Instant.parse("2026-04-12T00:00:00Z"))
                .payment(PaymentSummaryResponse.builder()
                        .paymentId(501L)
                        .status(paymentStatus)
                        .paymentMethod(PaymentMethod.UPI)
                        .amount(BigDecimal.valueOf(1000))
                        .gatewayReference("SIM-123")
                        .build())
                .build();
    }

    private OrderSummaryResponse orderSummaryResponse() {
        return OrderSummaryResponse.builder()
                .orderId(101L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(BigDecimal.valueOf(1000))
                .totalItems(2)
                .createdAt(Instant.parse("2026-04-12T00:00:00Z"))
                .build();
    }
}
