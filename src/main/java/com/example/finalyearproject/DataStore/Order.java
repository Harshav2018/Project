package com.example.finalyearproject.DataStore;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderId;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.CREATED;

    private double totalAmount;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date placedAt;

    // Shipping address details
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingZip;

    @ManyToOne
    @JoinColumn(name = "consumer_id", nullable = false)
    @JsonBackReference("consumer-order")
    private Consumer consumer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("order-items")
    private Set<OrderItem> orderItems = new HashSet<>();

    @OneToOne(mappedBy = "order")
    @JsonManagedReference("delivery-order")  // Add this reference
    private DeliveryAddresses deliveryAddress;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    // Helper method to place the order
    public void place() {
        this.orderStatus = OrderStatus.PLACED;
        this.placedAt = new Date();
    }

    // Helper method to recalculate order total
    public void recalculateTotal() {
        this.totalAmount = orderItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getUnitPrice())
                .sum();
    }
}