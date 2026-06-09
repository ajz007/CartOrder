package com.scaler.capstone.cartorder.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "Shipping address snapshot stored with the order")
public class ShippingAddressResponse {
    String fullName;
    String addressLine1;
    String addressLine2;
    String city;
    String state;
    String postalCode;
    String country;
    String phone;
}
