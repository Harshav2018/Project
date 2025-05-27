package com.example.finalyearproject.Controllers;

import com.example.finalyearproject.DataStore.Consumer;
import com.example.finalyearproject.DataStore.Order;
import com.example.finalyearproject.DataStore.OrderItem;
import com.example.finalyearproject.Services.CartService;
import com.example.finalyearproject.Services.ConsumerService;
import com.example.finalyearproject.Utility.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService; // You'll need to create this service

    @Autowired
    private ConsumerService consumerService;

    /**
     * Add item to cart
     */
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Set<OrderItem>>> addToCart(
            @RequestParam int productId,
            @RequestParam int quantity,
            Authentication authentication) {

        Consumer consumer = consumerService.findByEmail(authentication.getName());
        if (consumer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Consumer not found", "Authentication failed"));
        }

        ApiResponse<Set<OrderItem>> response =
                cartService.addToCart(consumer.getConsumerId(), productId, quantity);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove item from cart
     */
    @DeleteMapping("/{orderItemId}")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Set<OrderItem>>> removeFromCart(
            @PathVariable int orderItemId,
            @RequestParam int quantity,
            Authentication authentication) {

        Consumer consumer = consumerService.findByEmail(authentication.getName());
        if (consumer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Consumer not found", "Authentication failed"));
        }

        ApiResponse<Set<OrderItem>> response =
                cartService.removeFromCart(consumer.getConsumerId(), orderItemId, quantity);

        return ResponseEntity.ok(response);
    }

    /**
     * Get cart
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Order>> getCart(Authentication authentication) {
        Consumer consumer = consumerService.findByEmail(authentication.getName());
        if (consumer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Consumer not found", "Authentication failed"));
        }

        ApiResponse<Order> response = cartService.getCart(consumer.getConsumerId());
        return ResponseEntity.ok(response);
    }

    /**
     * Acknowledge cart changes
     */
    @PutMapping("/acknowledge-changes")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Set<OrderItem>>> acknowledgeChanges(Authentication authentication) {
        Consumer consumer = consumerService.findByEmail(authentication.getName());
        if (consumer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Consumer not found", "Authentication failed"));
        }

        ApiResponse<Set<OrderItem>> response =
                cartService.acknowledgeChanges(consumer.getConsumerId());

        return ResponseEntity.ok(response);
    }
}