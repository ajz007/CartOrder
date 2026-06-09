package com.scaler.capstone.cartorder.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Delivery address captured as an immutable order snapshot")
public class ShippingAddressRequest {

    @NotBlank(message = "shippingAddress.fullName is required")
    @Size(max = 120)
    @Schema(example = "Demo User")
    private String fullName;

    @NotBlank(message = "shippingAddress.addressLine1 is required")
    @Size(max = 255)
    @Schema(example = "12 Main Street")
    private String addressLine1;

    @Size(max = 255)
    @Schema(example = "Indiranagar")
    private String addressLine2;

    @NotBlank(message = "shippingAddress.city is required")
    @Size(max = 100)
    @Schema(example = "Bengaluru")
    private String city;

    @NotBlank(message = "shippingAddress.state is required")
    @Size(max = 100)
    @Schema(example = "Karnataka")
    private String state;

    @NotBlank(message = "shippingAddress.postalCode is required")
    @Pattern(regexp = "^[A-Za-z0-9 -]{3,12}$", message = "shippingAddress.postalCode is invalid")
    @Schema(example = "560001")
    private String postalCode;

    @NotBlank(message = "shippingAddress.country is required")
    @Size(max = 100)
    @Schema(example = "India")
    private String country;

    @NotBlank(message = "shippingAddress.phone is required")
    @Pattern(regexp = "^\\+?[0-9 -]{7,20}$", message = "shippingAddress.phone is invalid")
    @Schema(example = "+91 9876543210")
    private String phone;
}
