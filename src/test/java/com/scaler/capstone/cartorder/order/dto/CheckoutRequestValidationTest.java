package com.scaler.capstone.cartorder.order.dto;

import com.scaler.capstone.cartorder.payment.model.PaymentMethod;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresShippingAddress() {
        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.UPI);
        request.setSimulateSuccess(true);

        assertThat(validator.validate(request))
                .anySatisfy(violation ->
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("shippingAddress"));
    }

    @Test
    void validatesNestedShippingAddressFields() {
        CheckoutRequest request = new CheckoutRequest();
        request.setPaymentMethod(PaymentMethod.UPI);
        request.setSimulateSuccess(true);
        request.setShippingAddress(new ShippingAddressRequest());

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(
                        "shippingAddress.fullName",
                        "shippingAddress.addressLine1",
                        "shippingAddress.city",
                        "shippingAddress.state",
                        "shippingAddress.postalCode",
                        "shippingAddress.country",
                        "shippingAddress.phone"
                );
    }
}
