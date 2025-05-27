package com.example.finalyearproject.Controllers;


import com.example.finalyearproject.DataStore.*;
import com.example.finalyearproject.Services.ConsumerService;
import com.example.finalyearproject.Services.OrderService;
import com.example.finalyearproject.Services.ProductService;
import com.example.finalyearproject.Services.RatingServices;
import com.example.finalyearproject.Utility.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/consumer")
public class ConsumerController {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    private RatingServices ratingServices;

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        ApiResponse<List<Product>> response = productService.getAllProducts();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable int productId) {
        ApiResponse<Product> response = productService.getProductById(productId);

        if (response.getData() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }


    @GetMapping("products/category/{category}")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<List<Product>>> getProductsByCategory(@PathVariable String category) {
        ApiResponse<List<Product>> response = productService.getProductsByCategory(category);
        return ResponseEntity.ok(response);
    }

    @GetMapping("products/search")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<List<Product>>> searchProducts(@RequestParam String query) {
        ApiResponse<List<Product>> response = productService.searchProductsByName(query);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/getCategories")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<List<String>>> getCategoryList(){
        List<String> categories=new ArrayList<>();
        for (CategoryType value : CategoryType.values()) {
            categories.add(value.toString());
        }
        return ResponseEntity.ok(ApiResponse.success("categories fetched successfully",categories));
    }

    /**
     * Get consumer's order history
     */
    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<List<Order>>> getMyOrders(Authentication authentication) {
        String consumerEmail = authentication.getName();

        // Find consumer by email
        Consumer consumer = consumerService.findByEmail(consumerEmail);
        if (consumer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Consumer not found", "Authentication failed"));
        }

        // Get orders for this consumer
        List<Order> orders = orderService.getOrderHistory(consumer.getConsumerId());
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    /**
     * Get specific order details
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Order>> getMyOrderDetails(
            @PathVariable int orderId,
            Authentication authentication) {

        String consumerEmail = authentication.getName();

        // Find consumer
        Consumer consumer = consumerService.findByEmail(consumerEmail);
        if (consumer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Consumer not found", "Authentication failed"));
        }

        // Get order and verify ownership
        Order order = orderService.getOrderById(orderId);
        if (order == null || order.getConsumer().getConsumerId() != consumer.getConsumerId()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Order not found", "No such order found"));
        }

        return ResponseEntity.ok(ApiResponse.success("Order details retrieved", order));
    }

    @PutMapping("/orders/{orderId}/confirm")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Order>> confirmOrderReceipt(
            @PathVariable int orderId,
            Authentication authentication) {

        String consumerEmail = authentication.getName();
        ApiResponse<Order> response = orderService.confirmOrderReceipt(orderId, consumerEmail);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-ratings")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Set<Rating>>> getMyRatings(Authentication authentication) {
        String consumerEmail = authentication.getName();
        ApiResponse<Set<Rating>> response = ratingServices.getUserRatings(consumerEmail);
        if (response.getData() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}