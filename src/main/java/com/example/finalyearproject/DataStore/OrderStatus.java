package com.example.finalyearproject.DataStore;

public enum OrderStatus {
    CREATED,    // Cart/Draft
    PLACED,
    DELIVERED,  // Order has been delivered
    COMPLETED // Order confirmed

    // Future states can be added later: PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED
}
