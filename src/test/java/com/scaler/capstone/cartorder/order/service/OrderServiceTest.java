package com.scaler.capstone.cartorder.order.service;

import com.scaler.capstone.cartorder.cart.model.Cart;
import com.scaler.capstone.cartorder.cart.model.CartItem;
import com.scaler.capstone.cartorder.cart.model.CartStatus;
import com.scaler.capstone.cartorder.cart.repository.CartRepository;
import com.scaler.capstone.cartorder.event.OrderEventPublisher;
import com.scaler.capstone.cartorder.exception.ProductUnavailableException;
import com.scaler.capstone.cartorder.order.dto.CheckoutPreviewResponse;
import com.scaler.capstone.cartorder.order.dto.CheckoutRequest;
import com.scaler.capstone.cartorder.order.dto.OrderResponse;
import com.scaler.capstone.cartorder.order.dto.ShippingAddressRequest;
import com.scaler.capstone.cartorder.order.model.Order;
import com.scaler.capstone.cartorder.order.model.OrderStatus;
import com.scaler.capstone.cartorder.order.repository.OrderRepository;
import com.scaler.capstone.cartorder.payment.model.PaymentMethod;
import com.scaler.capstone.cartorder.payment.model.PaymentStatus;
import com.scaler.capstone.cartorder.payment.model.PaymentTransaction;
import com.scaler.capstone.cartorder.payment.repository.PaymentTransactionRepository;
import com.scaler.capstone.cartorder.product.client.ProductClient;
import com.scaler.capstone.cartorder.product.dto.ProductDetailsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
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
    @Mock
    private ProductClient productClient;

    private OrderService orderService;
    private Cart cart;

    @BeforeEach
    void setUp() {
        CheckoutPricingService pricingService = new CheckoutPricingService(
                productClient,
                new BigDecimal("0.18"),
                BigDecimal.ZERO
        );
        orderService = new OrderService(
                cartRepository,
                orderRepository,
                paymentTransactionRepository,
                orderEventPublisher,
                pricingService
        );

        cart = activeCart();
        lenient().when(cartRepository.findByUserIdAndStatus("demo@example.com", CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        lenient().when(cartRepository.findForCheckout("demo@example.com", CartStatus.ACTIVE))
                .thenReturn(Optional.of(cart));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(10L);
                order.prePersist();
            }
            return order;
        });
        lenient().when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(20L);
                payment.prePersist();
            }
            return payment;
        });
        lenient().when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void previewUsesCurrentCatalogPriceAndReturnsAmountBreakdown() {
        when(productClient.getProductById(1L)).thenReturn(product("100.00", 5));

        CheckoutPreviewResponse response = orderService.previewCheckout("demo@example.com");

        assertThat(response.getSubtotal()).isEqualByComparingTo("200.00");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("36.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("236.00");
        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getUnitPrice()).isEqualByComparingTo("100.00");
                    assertThat(item.getAvailableStock()).isEqualTo(5);
                });
    }

    @Test
    void successfulCheckoutSnapshotsAddressAndConsumesCart() {
        when(productClient.getProductById(1L)).thenReturn(product("100.00", 5));

        OrderResponse response = orderService.checkout("demo@example.com", checkoutRequest(true));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.getPayment().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getSubtotal()).isEqualByComparingTo("200.00");
        assertThat(response.getTaxAmount()).isEqualByComparingTo("36.00");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("236.00");
        assertThat(response.getShippingAddress().getCity()).isEqualTo("Bengaluru");
        assertThat(response.getItems()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getProductTitle()).isEqualTo("Current product title");
                    assertThat(item.getUnitPrice()).isEqualByComparingTo("100.00");
                });
        assertThat(cart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);
        verify(cartRepository).save(cart);
        verify(orderEventPublisher).publishOrderCreated(any());
    }

    @Test
    void failedPaymentLeavesCartActiveAndDoesNotPublishCreatedEvent() {
        when(productClient.getProductById(1L)).thenReturn(product("100.00", 5));

        OrderResponse response = orderService.checkout("demo@example.com", checkoutRequest(false));

        assertThat(response.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(response.getPayment().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        verify(cartRepository, never()).save(any());
        verify(orderEventPublisher, never()).publishOrderCreated(any());
    }

    @Test
    void checkoutRejectsInsufficientStockBeforeCreatingOrder() {
        when(productClient.getProductById(1L)).thenReturn(product("100.00", 1));

        assertThatThrownBy(() -> orderService.checkout("demo@example.com", checkoutRequest(true)))
                .isInstanceOf(ProductUnavailableException.class)
                .hasMessageContaining("requested 2, available 1");

        verify(orderRepository, never()).save(any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    private Cart activeCart() {
        Cart activeCart = new Cart();
        activeCart.setId(1L);
        activeCart.setUserId("demo@example.com");
        activeCart.setStatus(CartStatus.ACTIVE);

        CartItem item = new CartItem();
        item.setId(2L);
        item.setCart(activeCart);
        item.setProductId(1L);
        item.setProductTitleSnapshot("Old title");
        item.setUnitPrice(new BigDecimal("80.00"));
        item.setQuantity(2);
        item.setTotalPrice(new BigDecimal("160.00"));
        activeCart.getItems().add(item);
        return activeCart;
    }

    private ProductDetailsResponse product(String price, int stockQuantity) {
        ProductDetailsResponse product = new ProductDetailsResponse();
        product.setId(1L);
        product.setTitle("Current product title");
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stockQuantity);
        return product;
    }

    private CheckoutRequest checkoutRequest(boolean simulateSuccess) {
        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.UPI);
        request.setSimulateSuccess(simulateSuccess);
        request.setShippingAddress(shippingAddress());
        return request;
    }

    private ShippingAddressRequest shippingAddress() {
        ShippingAddressRequest address = new ShippingAddressRequest();
        address.setFullName("Demo User");
        address.setAddressLine1("12 Main Street");
        address.setCity("Bengaluru");
        address.setState("Karnataka");
        address.setPostalCode("560001");
        address.setCountry("India");
        address.setPhone("+91 9876543210");
        return address;
    }
}
