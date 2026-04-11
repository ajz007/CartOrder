package com.scaler.capstone.cartorder.cart.controller;

import com.scaler.capstone.cartorder.cart.dto.AddCartItemRequest;
import com.scaler.capstone.cartorder.cart.dto.CartResponse;
import com.scaler.capstone.cartorder.cart.dto.UpdateCartItemRequest;
import com.scaler.capstone.cartorder.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Cart", description = "Authenticated cart management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Adds a product to the active cart or increments quantity if it already exists.")
    public CartResponse addItem(Authentication authentication, @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(authentication.getName(), request);
    }

    @GetMapping
    @Operation(summary = "Get active cart", description = "Returns the current active cart for the authenticated user.")
    public CartResponse getCart(Authentication authentication) {
        return cartService.getCurrentCart(authentication.getName());
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of an item in the current user's active cart.")
    public CartResponse updateItem(
            Authentication authentication,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItem(authentication.getName(), itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove cart item", description = "Removes a single item from the current user's active cart.")
    public CartResponse removeItem(Authentication authentication, @PathVariable Long itemId) {
        return cartService.removeItem(authentication.getName(), itemId);
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Removes all items from the current user's active cart.")
    public CartResponse clearCart(Authentication authentication) {
        return cartService.clearCart(authentication.getName());
    }
}
