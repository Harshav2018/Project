package com.example.finalyearproject.Controllers;

import com.example.finalyearproject.DataStore.Rating;
import com.example.finalyearproject.Services.RatingServices;
import com.example.finalyearproject.Utility.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/rating")
public class RatingController {

    @Autowired
    private RatingServices ratingServices;

    /**
     * Add a new rating for a product
     */
    @PostMapping("/{productId}")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Rating>> addRating(
            @PathVariable int productId,
            @RequestParam int orderItemId,  // Added parameter for order item
            @Valid @RequestBody Rating rating,
            Authentication authentication) {

        String consumerEmail = authentication.getName();
        ApiResponse<Rating> response = ratingServices.addRating(rating, consumerEmail, productId, orderItemId);

        if (response.getData() != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Update an existing rating
     */
    @PutMapping("/{ratingId}")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Rating>> updateRating(
            @PathVariable int ratingId,
            @Valid @RequestBody Rating rating,
            Authentication authentication) {

        String consumerEmail = authentication.getName();
        rating.setRatingId(ratingId); // Ensure the ID is set from the path variable

        ApiResponse<Rating> response = ratingServices.updateRating(rating, consumerEmail);

        if (response.getData() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Delete a rating
     */
    @DeleteMapping("/{ratingId}")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Void>> deleteRating(
            @PathVariable int ratingId,
            Authentication authentication) {

        String consumerEmail = authentication.getName();
        ApiResponse<Void> response = ratingServices.deleteRating(ratingId, consumerEmail);

        if (response.getErrors() == null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Get a specific rating by ID
     */
    @GetMapping("/{ratingId}")
    public ResponseEntity<ApiResponse<Rating>> getRatingById(@PathVariable int ratingId) {
        ApiResponse<Rating> response = ratingServices.getRatingById(ratingId);

        if (response.getData() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Get all ratings for a specific product
     */
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Set<Rating>>> getProductRatings(@PathVariable int productId) {
        ApiResponse<Set<Rating>> response = ratingServices.getProductRatings(productId);

        if (response.getData() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Get all ratings by the authenticated user
     */


    /**
     * Check if a specific order item can be rated
     */
    @GetMapping("/can-rate/{orderItemId}")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Boolean>> canRateOrderItem(
            @PathVariable int orderItemId,
            Authentication authentication) {

        String consumerEmail = authentication.getName();
        ApiResponse<Boolean> response = ratingServices.canRateOrderItem(orderItemId, consumerEmail);
        return ResponseEntity.ok(response);
    }
}