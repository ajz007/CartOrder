package com.scaler.capstone.cartorder.exception;

public class CartAccessDeniedException extends RuntimeException {
    public CartAccessDeniedException(String message) {
        super(message);
    }
}
