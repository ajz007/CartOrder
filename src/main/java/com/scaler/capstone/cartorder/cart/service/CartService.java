package com.scaler.capstone.cartorder.cart.service;

import com.scaler.capstone.cartorder.cart.dto.AddCartItemRequest;
import com.scaler.capstone.cartorder.cart.dto.CartItemResponse;
import com.scaler.capstone.cartorder.cart.dto.CartResponse;
import com.scaler.capstone.cartorder.cart.dto.UpdateCartItemRequest;
import com.scaler.capstone.cartorder.cart.model.Cart;
import com.scaler.capstone.cartorder.cart.model.CartItem;
import com.scaler.capstone.cartorder.cart.model.CartStatus;
import com.scaler.capstone.cartorder.cart.repository.CartItemRepository;
import com.scaler.capstone.cartorder.cart.repository.CartRepository;
import com.scaler.capstone.cartorder.exception.CartAccessDeniedException;
import com.scaler.capstone.cartorder.exception.CartItemNotFoundException;
import com.scaler.capstone.cartorder.product.client.ProductClient;
import com.scaler.capstone.cartorder.product.dto.ProductDetailsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductClient productClient
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productClient = productClient;
    }

    @Transactional
    public CartResponse addItem(String userId, AddCartItemRequest request) {
        Cart cart = getOrCreateActiveCart(userId);
        ProductDetailsResponse product = productClient.getProductById(request.getProductId());

        CartItem item = cartItemRepository.findByCartAndProductId(cart, request.getProductId())
                .orElseGet(() -> createCartItem(cart, product, request.getProductId()));

        item.setProductTitleSnapshot(product.getTitle());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(item.getQuantity() + request.getQuantity());
        recalculateItem(item);
        cartItemRepository.save(item);

        return toResponse(cartRepository.save(cart));
    }

    @Transactional(readOnly = true)
    public CartResponse getCurrentCart(String userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .map(this::toResponse)
                .orElseGet(() -> emptyCartResponse(userId));
    }

    @Transactional
    public CartResponse updateItem(String userId, Long itemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found: " + itemId));
        Cart cart = getOwnedActiveCart(userId, item);

        item.setQuantity(request.getQuantity());
        recalculateItem(item);
        cartItemRepository.save(item);

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(String userId, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found: " + itemId));
        Cart cart = getOwnedActiveCart(userId, item);

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse clearCart(String userId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElse(null);
        if (cart == null) {
            return emptyCartResponse(userId);
        }
        cart.getItems().clear();
        cartItemRepository.deleteByCart(cart);
        return toResponse(cartRepository.save(cart));
    }

    private CartItem createCartItem(Cart cart, ProductDetailsResponse product, Long productId) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductId(productId);
        item.setProductTitleSnapshot(product.getTitle());
        item.setUnitPrice(product.getPrice());
        item.setQuantity(0);
        item.setTotalPrice(BigDecimal.ZERO);
        cart.getItems().add(item);
        return item;
    }

    private Cart getOrCreateActiveCart(String userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUserId(userId);
                    cart.setStatus(CartStatus.ACTIVE);
                    return cartRepository.save(cart);
                });
    }

    private Cart getOwnedActiveCart(String userId, CartItem item) {
        Cart cart = item.getCart();
        if (cart == null) {
            throw new CartItemNotFoundException("Cart item not found: " + item.getId());
        }
        if (!userId.equals(cart.getUserId())) {
            throw new CartAccessDeniedException("Cart item does not belong to the current user");
        }
        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new CartItemNotFoundException("Cart item not found: " + item.getId());
        }
        return cart;
    }

    private void recalculateItem(CartItem item) {
        item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = items.stream()
                .map(CartItemResponse::getQuantity)
                .reduce(0, Integer::sum);

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .status(cart.getStatus())
                .items(items)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .empty(items.isEmpty())
                .build();
    }

    private CartResponse emptyCartResponse(String userId) {
        return CartResponse.builder()
                .cartId(null)
                .userId(userId)
                .status(CartStatus.ACTIVE)
                .items(List.of())
                .totalAmount(BigDecimal.ZERO)
                .totalItems(0)
                .empty(true)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .itemId(item.getId())
                .productId(item.getProductId())
                .productTitle(item.getProductTitleSnapshot())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}
