package com.scaler.capstone.cartorder.controller;

import com.scaler.capstone.cartorder.exception.AuthException;
import com.scaler.capstone.cartorder.exception.CartAccessDeniedException;
import com.scaler.capstone.cartorder.exception.CartItemNotFoundException;
import com.scaler.capstone.cartorder.exception.DependentServiceException;
import com.scaler.capstone.cartorder.exception.EmptyCartCheckoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesUnauthorized() {
        assertThat(handler.handleUnauthorized(new AuthException("Unauthorized")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handlesBadRequest() {
        assertThat(handler.handleBadRequest(new IllegalArgumentException("bad")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handlesInvalidCheckoutState() {
        assertThat(handler.handleInvalidCheckoutState(new EmptyCartCheckoutException("empty")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handlesNotFound() {
        assertThat(handler.handleNotFound(new CartItemNotFoundException("missing")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handlesForbidden() {
        assertThat(handler.handleForbidden(new CartAccessDeniedException("forbidden")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handlesDependencyFailure() {
        assertThat(handler.handleDependencyFailure(new DependentServiceException("down")).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void handlesGenericError() {
        assertThat(handler.handleGeneric(new RuntimeException("boom")).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
