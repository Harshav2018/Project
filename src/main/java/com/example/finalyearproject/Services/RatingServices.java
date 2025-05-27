package com.example.finalyearproject.Services;

import com.example.finalyearproject.Abstraction.*;
import com.example.finalyearproject.DataStore.*;
import com.example.finalyearproject.Utility.ApiResponse;
import com.example.finalyearproject.customExceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RatingServices {

    private static final Logger logger = LoggerFactory.getLogger(RatingServices.class);

    @Autowired
    private RatingRepo ratingRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private ConsumerRepo consumerRepo;

    @Autowired
    private FarmerRepo farmerRepo;

    @Autowired
    private OrderItemRepo orderItemRepo;

    // Helper method to update the farmer's aggregates incrementally on add/update/delete
    private void updateFarmerAggregates(Farmer farmer, double scoreDelta, int countDelta) {
        // Get current aggregates, ensuring they are not null
        double currentTotal = farmer.getTotalRating() != null ? farmer.getTotalRating() : 0.0;
        int currentCount = farmer.getRatingCount() != null ? farmer.getRatingCount() : 0;

        currentTotal += scoreDelta;
        currentCount += countDelta;

        double newAverage = currentCount > 0 ? currentTotal / currentCount : 0.0;

        farmer.setTotalRating(currentTotal);
        farmer.setRatingCount(currentCount);
        farmer.setAverageRating(newAverage);

        farmerRepo.save(farmer);
    }

    private void updateProductAggregates(Product product, double scoreDelta, int countDelta) {
        // Get current aggregates, ensuring they are not null
        double currentTotal = product.getTotalRating() != null ? product.getTotalRating() : 0.0;
        int currentCount = product.getRatingCount() != null ? product.getRatingCount() : 0;

        currentTotal += scoreDelta;
        currentCount += countDelta;

        double newAverage = currentCount > 0 ? currentTotal / currentCount : 0.0;

        product.setTotalRating(currentTotal);
        product.setRatingCount(currentCount);
        product.setAverageRating(newAverage);

        productRepo.save(product);
    }

    @Transactional
    public ApiResponse<Rating> addRating(Rating rating, String consumerEmail, int productId, int orderItemId) {
        try {
            // Get consumer
            Consumer consumer = consumerRepo.findByConsumerEmail(consumerEmail);
            if (consumer == null) {
                return ApiResponse.error("Failed to add rating", "Consumer not found with email: " + consumerEmail);
            }

            // Get product
            Product product;
            try {
                product = productRepo.findById(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            } catch (ResourceNotFoundException e) {
                return ApiResponse.error("Failed to add rating", e.getMessage());
            }

            // Get order item and validate
            OrderItem orderItem = orderItemRepo.findById(orderItemId).orElse(null);
            if (orderItem == null) {
                return ApiResponse.error("Failed to add rating", "Order item not found");
            }

            // Verify the order item belongs to this consumer
            Order order = orderItem.getOrder();
            if (order == null || order.getConsumer().getConsumerId() != consumer.getConsumerId()) {
                return ApiResponse.error("Unauthorized", "You don't have permission to rate this order item");
            }

            // Verify the order item contains this product
            if (orderItem.getProduct().getProductId() != productId) {
                return ApiResponse.error("Invalid request", "Order item does not match the specified product");
            }

            // Verify the order is completed
            if (order.getOrderStatus() != OrderStatus.COMPLETED) {
                return ApiResponse.error("Unauthorized", "You can only rate products from completed orders");
            }

            // Check if the item is already rated
            if (orderItem.isRated()) {
                return ApiResponse.error("Already rated", "This order item has already been rated");
            }

            // Check if user already rated this product (from any order)
            if (ratingRepo.existsByConsumer_ConsumerIdAndProduct_ProductId(consumer.getConsumerId(), productId)) {
                return ApiResponse.error("Rating exists", "You have already rated this product. Please update your existing rating.");
            }

            // Create and save the rating
            rating.setConsumer(consumer);
            rating.setProduct(product);
            Rating savedRating = ratingRepo.save(rating);

            // Mark the order item as rated
            orderItem.setRated(true);
            orderItemRepo.save(orderItem);

            // Update aggregates
            Farmer farmer = product.getFarmer();
            if (farmer != null) {
                updateFarmerAggregates(farmer, savedRating.getScore(), 1);
                updateProductAggregates(product, savedRating.getScore(), 1);
            }

            return ApiResponse.success("Rating added successfully", savedRating);
        } catch (Exception e) {
            logger.error("Failed to add rating: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to add rating", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Rating> updateRating(Rating updatedRating, String consumerEmail) {
        try {
            Consumer consumer = consumerRepo.findByConsumerEmail(consumerEmail);
            if (consumer == null) {
                return ApiResponse.error("Failed to update rating", "Consumer not found with email: " + consumerEmail);
            }

            Rating existingRating;
            try {
                existingRating = ratingRepo.findById(updatedRating.getRatingId())
                        .orElseThrow(() -> new ResourceNotFoundException("Rating not found with ID: " + updatedRating.getRatingId()));
            } catch (ResourceNotFoundException e) {
                return ApiResponse.error("Failed to update rating", e.getMessage());
            }

            if (existingRating.getConsumer() == null || existingRating.getConsumer().getConsumerId() != consumer.getConsumerId()) {
                return ApiResponse.error("Unauthorized", "You don't have permission to update this rating");
            }

            // Capture the old score to determine the delta
            double oldScore = existingRating.getScore();
            existingRating.setScore(updatedRating.getScore());
            existingRating.setComment(updatedRating.getComment());
            Rating savedRating = ratingRepo.save(existingRating);

            Product product = savedRating.getProduct();
            Farmer farmer = product.getFarmer();
            if (farmer != null) {
                // Update aggregates: subtract the old score and add the new one (count remains unchanged)
                updateFarmerAggregates(farmer, savedRating.getScore() - oldScore, 0);
                updateProductAggregates(product, savedRating.getScore() - oldScore, 0);
            }

            return ApiResponse.success("Rating updated successfully", savedRating);
        } catch (Exception e) {
            logger.error("Failed to update rating: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to update rating", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Void> deleteRating(int ratingId, String consumerEmail) {
        try {
            Consumer consumer = consumerRepo.findByConsumerEmail(consumerEmail);
            if (consumer == null) {
                return ApiResponse.error("Failed to delete rating", "Consumer not found with email: " + consumerEmail);
            }

            if (!ratingRepo.existsByConsumer_ConsumerIdAndRatingId(consumer.getConsumerId(), ratingId)) {
                return ApiResponse.error("Not found", "Rating not found with ID: " + ratingId + " for your account");
            }

            Rating ratingToDelete;
            try {
                ratingToDelete = ratingRepo.findById(ratingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Rating not found with ID: " + ratingId));
            } catch (ResourceNotFoundException e) {
                return ApiResponse.error("Failed to delete rating", e.getMessage());
            }

            Product product = ratingToDelete.getProduct();
            Farmer farmer = product.getFarmer();

            // Find and update order items that reference this rating
            List<OrderItem> orderItems = orderItemRepo.findByProductIdAndConsumerId(
                    product.getProductId(), consumer.getConsumerId());

            for (OrderItem item : orderItems) {
                if (item.isRated()) {
                    item.setRated(false);
                    orderItemRepo.save(item);
                }
            }

            ratingRepo.deleteById(ratingId);

            if (farmer != null) {
                // Subtract the rating's score and decrement the count
                updateFarmerAggregates(farmer, -ratingToDelete.getScore(), -1);
                updateProductAggregates(product, -ratingToDelete.getScore(), -1);
            }

            return ApiResponse.success("Rating deleted successfully");
        } catch (Exception e) {
            logger.error("Failed to delete rating: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to delete rating", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Set<Rating>> getProductRatings(int productId) {
        try {
            Product product;
            try {
                product = productRepo.findById(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
            } catch (ResourceNotFoundException e) {
                return ApiResponse.error("Failed to retrieve ratings", e.getMessage());
            }

            Set<Rating> ratings = product.getRatings();
            return ApiResponse.success("Ratings retrieved successfully", ratings);
        } catch (Exception e) {
            logger.error("Failed to get product ratings: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve ratings", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Rating> getRatingById(int ratingId) {
        try {
            Rating rating;
            try {
                rating = ratingRepo.findById(ratingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Rating not found with ID: " + ratingId));
            } catch (ResourceNotFoundException e) {
                return ApiResponse.error("Rating not found", e.getMessage());
            }

            return ApiResponse.success("Rating retrieved successfully", rating);
        } catch (Exception e) {
            logger.error("Failed to get rating: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve rating", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Set<Rating>> getUserRatings(String consumerEmail) {
        try {
            Consumer consumer = consumerRepo.findByConsumerEmail(consumerEmail);
            if (consumer == null) {
                return ApiResponse.error("Failed to retrieve ratings", "Consumer not found with email: " + consumerEmail);
            }

            Set<Rating> ratings = ratingRepo.findByConsumer_ConsumerId(consumer.getConsumerId());
            return ApiResponse.success("User ratings retrieved successfully", ratings);
        } catch (Exception e) {
            logger.error("Failed to get user ratings: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve ratings", e.getMessage());
        }
    }

    /**
     * Check if a specific order item can be rated
     */
    @Transactional
    public ApiResponse<Boolean> canRateOrderItem(int orderItemId, String consumerEmail) {
        try {
            Consumer consumer = consumerRepo.findByConsumerEmail(consumerEmail);
            if (consumer == null) {
                return ApiResponse.error("Failed to check rating ability", "Consumer not found");
            }

            OrderItem orderItem = orderItemRepo.findById(orderItemId).orElse(null);
            if (orderItem == null) {
                return ApiResponse.error("Failed to check rating ability", "Order item not found");
            }

            // Verify the order item belongs to this consumer
            Order order = orderItem.getOrder();
            if (order == null || order.getConsumer().getConsumerId() != consumer.getConsumerId()) {
                return ApiResponse.error("Unauthorized", "This order item doesn't belong to you");
            }

            // Check if order is completed and not yet rated
            boolean canRate = order.getOrderStatus() == OrderStatus.COMPLETED && !orderItem.isRated();

            return ApiResponse.success("Rating ability checked", canRate);
        } catch (Exception e) {
            logger.error("Failed to check rating ability: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to check rating ability", e.getMessage());
        }
    }
}