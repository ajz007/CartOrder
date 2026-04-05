package com.scaler.capstone.cartorder.product.client;

import com.scaler.capstone.cartorder.exception.DependentServiceException;
import com.scaler.capstone.cartorder.exception.ProductNotFoundException;
import com.scaler.capstone.cartorder.product.dto.ProductDetailsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Component
public class ProductClient {

    private final RestTemplate restTemplate;
    private final String productCatalogBaseUrl;

    public ProductClient(
            RestTemplate restTemplate,
            @Value("${app.product-catalog.base-url}") String productCatalogBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.productCatalogBaseUrl = productCatalogBaseUrl;
    }

    public ProductDetailsResponse getProductById(Long productId) {
        try {
            ProductDetailsResponse response = restTemplate.getForObject(
                    productCatalogBaseUrl + "/products/" + productId,
                    ProductDetailsResponse.class
            );
            if (response == null) {
                throw new DependentServiceException("Product catalog returned an empty response");
            }
            return response;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ProductNotFoundException("Product not found: " + productId);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ProductNotFoundException("Product not found: " + productId);
            }
            throw new DependentServiceException("Product catalog request failed");
        } catch (RestClientException ex) {
            throw new DependentServiceException("Product catalog is unavailable");
        }
    }
}
