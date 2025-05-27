package com.example.finalyearproject.Controllers;

import com.example.finalyearproject.DataStore.*;
import com.example.finalyearproject.Services.FarmerService;
import com.example.finalyearproject.Services.OrderService;
import com.example.finalyearproject.Services.ProductService;
import com.example.finalyearproject.Services.RatingServices;
import com.example.finalyearproject.Utility.ApiResponse;
import com.example.finalyearproject.Utility.FarmerUpdateDTO;
import com.example.finalyearproject.Utility.FarmerUtility;
import com.example.finalyearproject.Utility.ProductResponseUtility;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/farmer")
public class FarmerController {

    @Autowired
    private FarmerService farmerService;

    @Autowired
    private ProductService productService;

    @Autowired
    private RatingServices ratingServices;

    @Autowired
    private OrderService orderService;



    @PutMapping("/update-profile")
    @PreAuthorize("hasAuthority('FARMER')")
    public ResponseEntity<ApiResponse<FarmerUtility>> updateFarmer(
            @Valid @RequestBody FarmerUpdateDTO updateDTO,
            Authentication authentication) {

        String farmerEmail = authentication.getName();
        ApiResponse<FarmerUtility> response = farmerService.updateFarmer(updateDTO, farmerEmail);

        if (response.getData() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

//    @GetMapping("/my-products")
//    @PreAuthorize("hasAuthority('FARMER')")
//    public ResponseEntity<ApiResponse<List<Product>>> getMyProducts(Authentication authentication) {
//        String farmerEmail = authentication.getName();
//        ApiResponse<List<Product>> response = productService.getProductsByFarmerEmail(farmerEmail);
//        return ResponseEntity.ok(response);
//    }

    // Alternative formatted response if you prefer ProductResponseUtility
    @GetMapping("/products")
    @PreAuthorize("hasAuthority('FARMER')")
    public ResponseEntity<ApiResponse<Set<ProductResponseUtility>>> GetAllProductsFormatted() {
        String farmerEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        ApiResponse<List<Product>> productsResponse = productService.getProductsByFarmerEmail(farmerEmail);

        if (productsResponse.getData() != null) {
            Set<ProductResponseUtility> formattedProducts = productsResponse.getData().stream()
                    .map(product -> ProductResponseUtility.builder()
                            .productId(product.getProductId())
                            .description(product.getDescription())
                            .stock(product.getStock())
                            .price(product.getPrice())
                            .name(product.getName())
                            .category(product.getCategory().toString())
                            .harvestDate(product.getHarvestDate())
                            .availableDate(product.getAvailableFromDate())
                            .imageUrls(product.getImages().stream()
                                    .map(ProductImage::getFilePath) // No need to hardcode localhost
                                    .collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toSet());

            return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", formattedProducts));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to retrieve products", productsResponse.getMessage()));
        }
    }
    /**
     * Delete a product (farmer only)
     */

    @GetMapping("/ratings/{productId}")
    @PreAuthorize("hasAuthority('FARMER')")
    public ResponseEntity<ApiResponse<Set<Rating>>> getProductRatings(@PathVariable int productId) {
        try {
            ApiResponse<Set<Rating>> response = ratingServices.getProductRatings(productId);

            if (response.getData() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(response);
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve ratings", e.getMessage()));
        }
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<String>> exceptionHandler() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication failed", "Invalid credentials"));
    }

    /**
     * Mark order as delivered (fix the path)
     */
    @PutMapping("/orders/{orderId}/deliver") // Remove redundant "farmer" from path
    @PreAuthorize("hasAuthority('FARMER')")
    public ResponseEntity<ApiResponse<Order>> markOrderDelivered(
            @PathVariable int orderId,
            Authentication authentication) {

        String farmerEmail = authentication.getName();
        ApiResponse<Order> response = orderService.markOrderDelivered(orderId, farmerEmail);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all orders containing products from this farmer
     */
    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('FARMER')")
    public ResponseEntity<ApiResponse<List<Order>>> getFarmerOrders(Authentication authentication) {
        String farmerEmail = authentication.getName();
        ApiResponse<List<Order>> response = orderService.getFarmerOrders(farmerEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Get specific order details (if it contains farmer's products)
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAuthority('FARMER')")
    public ResponseEntity<ApiResponse<Order>> getFarmerOrderDetails(
            @PathVariable int orderId,
            Authentication authentication) {

        String farmerEmail = authentication.getName();
        ApiResponse<Order> response = orderService.getFarmerOrderDetails(orderId, farmerEmail);

        if (response.getData() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}