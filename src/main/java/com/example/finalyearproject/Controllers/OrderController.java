package com.example.finalyearproject.Controllers;

import com.example.finalyearproject.DataStore.Consumer;
import com.example.finalyearproject.DataStore.Order;
import com.example.finalyearproject.Services.ConsumerService;
import com.example.finalyearproject.Services.OrderService;
import com.example.finalyearproject.Utility.ApiResponse;
import com.example.finalyearproject.Utility.OrderPlacementDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ConsumerService consumerService;

    /**
     * Place order
     */
    @PostMapping("/place")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Order>> placeOrder(
            @Valid @RequestBody OrderPlacementDTO placementDTO,
            Authentication authentication) {

        Consumer consumer = consumerService.findByEmail(authentication.getName());
        if (consumer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Consumer not found", "Authentication failed"));
        }

        ApiResponse<Order> response = orderService.placeOrder(consumer.getConsumerId(), placementDTO);

        if (response.getData() != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Get order history
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<List<Order>>> getOrderHistory(Authentication authentication) {
        Consumer consumer = consumerService.findByEmail(authentication.getName());
        if (consumer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Consumer not found", "Authentication failed"));
        }

        List<Order> orders = orderService.getOrderHistory(consumer.getConsumerId());
        return ResponseEntity.ok(ApiResponse.success("Order history retrieved", orders));
    }

    /**
     * Get order details
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Order>> getOrderDetails(
            @PathVariable int orderId,
            Authentication authentication) {

        Consumer consumer = consumerService.findByEmail(authentication.getName());
        if (consumer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Consumer not found", "Authentication failed"));
        }

        Order order = orderService.getOrderById(orderId);

        // Verify order belongs to this consumer
        if (order == null || order.getConsumer().getConsumerId() != consumer.getConsumerId()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Order not found", "No such order found"));
        }

        return ResponseEntity.ok(ApiResponse.success("Order details retrieved", order));
    }
}