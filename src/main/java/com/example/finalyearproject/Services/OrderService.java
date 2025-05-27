package com.example.finalyearproject.Services;

import com.example.finalyearproject.Abstraction.*;
import com.example.finalyearproject.DataStore.*;
import com.example.finalyearproject.Utility.ApiResponse;
import com.example.finalyearproject.Utility.OrderPlacementDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);


    @Autowired
    private OrderRepo orderRepo;


    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private FarmerRepo farmerRepo;

    /**
     * Place order and reduce product stock
     * Uses higher isolation level to prevent overselling
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ApiResponse<Order> placeOrder(int consumerId, OrderPlacementDTO placementDTO) {
        try {
            // Get active cart
            Order cart = orderRepo.findActiveCartByConsumerId(consumerId);
            if (cart == null || cart.getOrderItems().isEmpty()) {
                return ApiResponse.error("Empty cart", "Your cart is empty");
            }

            // Check if all items are still in stock
            StringBuilder stockErrors = new StringBuilder();
            boolean hasStockError = false;

            for (OrderItem item : cart.getOrderItems()) {
                // Reload product to get latest stock
                Product product = productRepo.findById(item.getProduct().getProductId()).orElse(null);
                if (product == null) {
                    stockErrors.append("Product ").append(item.getProduct().getName())
                            .append(" is no longer available. ");
                    hasStockError = true;
                    continue;
                }

                if (product.getStock() < item.getQuantity()) {
                    stockErrors.append("Only ").append(product.getStock())
                            .append(" units available for ").append(product.getName())
                            .append(" (requested: ").append(item.getQuantity()).append("). ");
                    hasStockError = true;
                }
            }

            if (hasStockError) {
                return ApiResponse.error("Insufficient stock", stockErrors.toString());
            }

            // Set shipping information
            if (placementDTO != null) {
                cart.setShippingAddress(placementDTO.getShippingAddress());
                cart.setShippingCity(placementDTO.getShippingCity());
                cart.setShippingState(placementDTO.getShippingState());
                cart.setShippingZip(placementDTO.getShippingZip());
            }

            // Update stock for each product
            for (OrderItem item : cart.getOrderItems()) {
                Product product = item.getProduct();
                int newStock = product.getStock() - item.getQuantity();
                product.setStock(newStock);
                productRepo.save(product);
            }

            // Update order status
            cart.place(); // Sets status to PLACED and timestamps
            Order placedOrder = orderRepo.save(cart);

            return ApiResponse.success("Order placed successfully", placedOrder);
        } catch (Exception e) {
            logger.error("Failed to place order: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to place order", e.getMessage());
        }
    }

    /**
     * Get order history for a consumer
     */
    public List<Order> getOrderHistory(int consumerId) {
        return orderRepo.findByConsumer_ConsumerIdOrderByCreatedAtDesc(consumerId);
    }

    /**
     * Get order by ID
     */
    public Order getOrderById(int orderId) {
        return orderRepo.findById(orderId).orElse(null);
    }

    @Transactional
    public ApiResponse<Order> markOrderDelivered(int orderId, String farmerEmail) {
        try {
            Order order = orderRepo.findById(orderId).orElse(null);
            if (order == null) {
                return ApiResponse.error("Update failed", "Order not found");
            }

            // Simple check if any product in the order belongs to this farmer
            boolean hasProductFromFarmer = order.getOrderItems().stream()
                    .anyMatch(item -> item.getProduct().getFarmer().getFarmerEmail().equalsIgnoreCase(farmerEmail));

            if (!hasProductFromFarmer) {
                return ApiResponse.error("Update failed", "Unauthorized to update this order");
            }

            // Only allow updating PLACED orders
            if (order.getOrderStatus() != OrderStatus.PLACED) {
                return ApiResponse.error("Update failed", "Order is not in PLACED status");
            }

            order.setOrderStatus(OrderStatus.DELIVERED);
            Order updatedOrder = orderRepo.save(order);

            return ApiResponse.success("Order marked as delivered", updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to mark order as delivered: {}", e.getMessage(), e);
            return ApiResponse.error("Update failed", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Order> confirmOrderReceipt(int orderId, String consumerEmail) {
        try {
            Order order = orderRepo.findById(orderId).orElse(null);
            if (order == null) {
                return ApiResponse.error("Update failed", "Order not found");
            }

            // Verify order belongs to this consumer
            if (!order.getConsumer().getConsumerEmail().equalsIgnoreCase(consumerEmail)) {
                return ApiResponse.error("Update failed", "You don't have permission to update this order");
            }

            // Only allow confirming DELIVERED orders
            if (order.getOrderStatus() != OrderStatus.DELIVERED) {
                return ApiResponse.error("Update failed", "Order is not in DELIVERED status");
            }

            order.setOrderStatus(OrderStatus.COMPLETED);
            Order updatedOrder = orderRepo.save(order);

            return ApiResponse.success("Order receipt confirmed", updatedOrder);
        } catch (Exception e) {
            logger.error("Failed to confirm order receipt: {}", e.getMessage(), e);
            return ApiResponse.error("Update failed", e.getMessage());
        }
    }

    /**
     * Get all orders containing products from a specific farmer with PLACED status
     */
    public ApiResponse<List<Order>> getFarmerOrders(String farmerEmail) {
        try {
            // Verify farmer exists
            Farmer farmer = farmerRepo.findByFarmerEmail(farmerEmail);
            if (farmer == null) {
                return ApiResponse.error("Failed to retrieve orders", "Farmer not found");
            }

            List<Order> orders = orderRepo.findPlacedOrdersContainingFarmerProducts(farmerEmail);
            return ApiResponse.success("Farmer orders retrieved successfully", orders);
        } catch (Exception e) {
            logger.error("Failed to retrieve farmer orders: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve orders", e.getMessage());
        }
    }

    /**
     * Get details for a specific order if it contains products from this farmer
     */
    public ApiResponse<Order> getFarmerOrderDetails(int orderId, String farmerEmail) {
        try {
            Order order = orderRepo.findById(orderId).orElse(null);
            if (order == null) {
                return ApiResponse.error("Order not found", "No order found with ID: " + orderId);
            }

            // Check if order contains any products from this farmer
            boolean hasProductFromFarmer = order.getOrderItems().stream()
                    .anyMatch(item -> item.getProduct().getFarmer().getFarmerEmail().equalsIgnoreCase(farmerEmail));

            if (!hasProductFromFarmer) {
                return ApiResponse.error("Access denied", "This order does not contain your products");
            }

            return ApiResponse.success("Order details retrieved", order);
        } catch (Exception e) {
            logger.error("Failed to retrieve order details: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve order details", e.getMessage());
        }
    }
}