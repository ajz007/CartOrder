package com.scaler.capstone.cartorder.exception;

public class EmptyCartCheckoutException extends RuntimeException {
    public EmptyCartCheckoutException(String message) {
        super(message);
    }
}
