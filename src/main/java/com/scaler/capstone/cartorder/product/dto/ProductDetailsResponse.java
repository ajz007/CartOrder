package com.scaler.capstone.cartorder.product.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDetailsResponse {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String category;
    private Integer stockQuantity;
}
