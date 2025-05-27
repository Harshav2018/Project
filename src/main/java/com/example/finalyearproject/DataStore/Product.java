package com.example.finalyearproject.DataStore;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int productId;

    @NotBlank(message = "Product name cannot be null")
    @Column(length = 100)
    private String name;

    @NotBlank(message = " Description cannot be null")
    @Lob
    private String description;

    @NotNull(message = "Price cannot be null")
    @Positive
    private double price;

    @NotNull(message = "stock cannot be null")
    @Positive
    @Min(value = 0)
    private int stock;


    @JsonIgnore
    private LocalDate harvestDate;

    @JsonIgnore
    private LocalDate availableFromDate;

    @JsonIgnore
    private boolean isOrganic;

    @ManyToOne
    @JoinColumn(name = "farmer_id", nullable = false)
    @JsonBackReference("farmer-product")
    private Farmer farmer;


    @OneToMany(mappedBy = "product",cascade=CascadeType.ALL)
    @JsonManagedReference("product-ratings")
    private Set<Rating> ratings = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonManagedReference("order-product")
    private Set<OrderItem> orderItems;

    // One product can have multiple images.
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("product-images")
    private Set<ProductImage> images = new HashSet<>();

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    private CategoryType category;

    // Aggregate rating fields
    @JsonIgnore
    private Double totalRating = 0.0;
    @JsonIgnore
    private Integer ratingCount = 0;
    @JsonIgnore
    private Double averageRating = 0.0;
}
