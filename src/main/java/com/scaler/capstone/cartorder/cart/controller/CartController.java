package com.scaler.capstone.cartorder.cart.controller;

import com.scaler.capstone.cartorder.cart.dto.AddCartItemRequest;
import com.scaler.capstone.cartorder.cart.dto.CartResponse;
import com.scaler.capstone.cartorder.cart.dto.UpdateCartItemRequest;
import com.scaler.capstone.cartorder.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public CartResponse addItem(Authentication authentication, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(authentication.getName(), request);
    }

    @GetMapping
    public CartResponse getCart(Authentication authentication) {
        return cartService.getCurrentCart(authentication.getName());
    }

    @PutMapping("/items/{itemId}")
    public CartResponse updateItem(
            Authentication authentication,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItem(authentication.getName(), itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(Authentication authentication, @PathVariable Long itemId) {
        return cartService.removeItem(authentication.getName(), itemId);
    }

    @DeleteMapping
    public CartResponse clearCart(Authentication authentication) {
        return cartService.clearCart(authentication.getName());
    }
}
