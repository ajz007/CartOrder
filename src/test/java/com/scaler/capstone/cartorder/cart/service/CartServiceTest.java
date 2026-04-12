package com.scaler.capstone.cartorder.cart.service;

import com.scaler.capstone.cartorder.cart.dto.AddCartItemRequest;
import com.scaler.capstone.cartorder.cart.dto.CartResponse;
import com.scaler.capstone.cartorder.cart.dto.UpdateCartItemRequest;
import com.scaler.capstone.cartorder.cart.model.Cart;
import com.scaler.capstone.cartorder.cart.model.CartItem;
import com.scaler.capstone.cartorder.cart.model.CartStatus;
import com.scaler.capstone.cartorder.cart.repository.CartItemRepository;
import com.scaler.capstone.cartorder.cart.repository.CartRepository;
import com.scaler.capstone.cartorder.exception.CartAccessDeniedException;
import com.scaler.capstone.cartorder.product.client.ProductClient;
import com.scaler.capstone.cartorder.product.dto.ProductDetailsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItemCreatesCartAndAddsItem() {
        AddCartItemRequest request = addRequest(1L, 2);
        ProductDetailsResponse product = product(1L, "Phone", 500);
        Cart savedCart = activeCart("user-1", 1L);
        when(cartRepository.findByUserIdAndStatus("user-1", CartStatus.ACTIVE)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            if (cart.getId() == null) {
                cart.setId(1L);
            }
            return cart;
        });
        when(productClient.getProductById(1L)).thenReturn(product);
        when(cartItemRepository.findByCartAndProductId(any(Cart.class), any(Long.class))).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CartResponse response = cartService.addItem("user-1", request);

        assertThat(response.getCartId()).isEqualTo(1L);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("1000");
    }

    @Test
    void addItemIncrementsExistingItem() {
        Cart cart = activeCart("user-1", 1L);
        CartItem item = cartItem(cart, 11L, 1L, "Phone", 500, 1);
        cart.getItems().add(item);
        when(cartRepository.findByUserIdAndStatus("user-1", CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(productClient.getProductById(1L)).thenReturn(product(1L, "Phone", 500));
        when(cartItemRepository.findByCartAndProductId(cart, 1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(cart)).thenReturn(cart);

        CartResponse response = cartService.addItem("user-1", addRequest(1L, 2));

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("1500");
    }

    @Test
    void getCurrentCartReturnsEmptyResponseWhenMissing() {
        when(cartRepository.findByUserIdAndStatus("user-1", CartStatus.ACTIVE)).thenReturn(Optional.empty());

        CartResponse response = cartService.getCurrentCart("user-1");

        assertThat(response.isEmpty()).isTrue();
        assertThat(response.getCartId()).isNull();
        assertThat(response.getTotalItems()).isZero();
    }

    @Test
    void updateItemUpdatesQuantity() {
        Cart cart = activeCart("user-1", 1L);
        CartItem item = cartItem(cart, 11L, 1L, "Phone", 500, 1);
        cart.getItems().add(item);
        when(cartItemRepository.findById(11L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(cart)).thenReturn(cart);

        CartResponse response = cartService.updateItem("user-1", 11L, updateRequest(4));

        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(4);
        assertThat(response.getItems().get(0).getTotalPrice()).isEqualByComparingTo("2000");
    }

    @Test
    void removeItemDeletesOwnedItem() {
        Cart cart = activeCart("user-1", 1L);
        CartItem item = cartItem(cart, 11L, 1L, "Phone", 500, 1);
        cart.getItems().add(item);
        when(cartItemRepository.findById(11L)).thenReturn(Optional.of(item));
        doNothing().when(cartItemRepository).delete(item);
        when(cartRepository.save(cart)).thenReturn(cart);

        CartResponse response = cartService.removeItem("user-1", 11L);

        assertThat(response.getItems()).isEmpty();
        verify(cartItemRepository).delete(item);
    }

    @Test
    void clearCartDeletesAllItems() {
        Cart cart = activeCart("user-1", 1L);
        cart.getItems().add(cartItem(cart, 11L, 1L, "Phone", 500, 1));
        when(cartRepository.findByUserIdAndStatus("user-1", CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        doNothing().when(cartItemRepository).deleteByCart(cart);
        when(cartRepository.save(cart)).thenReturn(cart);

        CartResponse response = cartService.clearCart("user-1");

        assertThat(response.isEmpty()).isTrue();
        verify(cartItemRepository).deleteByCart(cart);
    }

    @Test
    void updateItemRejectsOwnershipMismatch() {
        Cart cart = activeCart("other-user", 1L);
        CartItem item = cartItem(cart, 11L, 1L, "Phone", 500, 1);
        when(cartItemRepository.findById(11L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.updateItem("user-1", 11L, updateRequest(2)))
                .isInstanceOf(CartAccessDeniedException.class)
                .hasMessage("Cart item does not belong to the current user");
    }

    private AddCartItemRequest addRequest(Long productId, Integer quantity) {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }

    private UpdateCartItemRequest updateRequest(Integer quantity) {
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(quantity);
        return request;
    }

    private ProductDetailsResponse product(Long id, String title, int price) {
        ProductDetailsResponse response = new ProductDetailsResponse();
        response.setId(id);
        response.setTitle(title);
        response.setPrice(BigDecimal.valueOf(price));
        return response;
    }

    private Cart activeCart(String userId, Long id) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUserId(userId);
        cart.setStatus(CartStatus.ACTIVE);
        return cart;
    }

    private CartItem cartItem(Cart cart, Long id, Long productId, String title, int price, int quantity) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setCart(cart);
        item.setProductId(productId);
        item.setProductTitleSnapshot(title);
        item.setUnitPrice(BigDecimal.valueOf(price));
        item.setQuantity(quantity);
        item.setTotalPrice(BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(quantity)));
        return item;
    }
}
