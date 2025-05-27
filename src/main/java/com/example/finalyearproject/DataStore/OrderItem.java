package com.example.finalyearproject.DataStore;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderItemId; // Changed to camelCase

    @NotNull(message = "Quantity cannot be null")
    private int quantity; // Changed to camelCase

    @NotNull(message = "UnitPrice cannot be null")
    private double unitPrice; // Changed to camelCase

    @ManyToOne()
    @JsonBackReference("order-items")
    private Order order;

    private String fieldChange; // Changed to camelCase

    @Column(nullable = false)
    private boolean isRated = false;

    // In your OrderItem entity or DTO class
    @JsonProperty("productId")  // Add this for serialization
    public Integer getProductId() {
        return product != null ? product.getProductId() : null;
    }

    // In your OrderItem entity or DTO class
    @JsonProperty("productName")  // Add this for serialization
    public String getProductName() {
        return product != null ? product.getName() : null;
    }

    @ManyToOne
    @JoinColumn(name = "product_id") // explicitly specify join column, if needed.
    @JsonBackReference("order-product")
    private Product product;

}
