package com.example.finalyearproject.DataStore;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ratingId;

    @NotNull(message = "Score cannot be null")
    @Min(value = 1, message = "Score must be at least 1")
    @Max(value = 5, message = "Score cannot exceed 5")
    private int score; // Changed to lowercase

    @NotBlank(message = "Comment cannot be null")
    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment; // Changed to lowercase

    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "consumer_id")
    @JsonBackReference("consumer-ratings")
    private Consumer consumer;

    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonBackReference("product-ratings")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "order_item_id")
    @JsonBackReference("orderitem-ratings")
    private OrderItem orderItem;

    // Safer toString implementation
    @Override
    public String toString() {
        return "Rating{" +
                "ratingId=" + ratingId +
                ", score=" + score +
                ", comment='" + comment + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}