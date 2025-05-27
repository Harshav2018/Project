package com.example.finalyearproject.Services;

import com.example.finalyearproject.Abstraction.ConsumerRepo;
import com.example.finalyearproject.Abstraction.OrderItemRepo;
import com.example.finalyearproject.Abstraction.OrderRepo;
import com.example.finalyearproject.Abstraction.ProductRepo;
import com.example.finalyearproject.DataStore.*;
import com.example.finalyearproject.Utility.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

@Service
public class CartService {

    @Autowired
    private OrderItemRepo orderItemRepo;

    @Autowired
    private ConsumerRepo consumerRepo;


    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private OrderRepo orderRepo;

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);


    /**
     * Add product to cart
     */
    @Transactional
    public ApiResponse<Set<OrderItem>> addToCart(int consumerId, int productId, int quantity) {
        try {
            // Validation
            if (quantity <= 0) {
                return ApiResponse.error("Invalid quantity", "Quantity must be greater than zero");
            }

            // Get consumer
            Consumer consumer = consumerRepo.findById(consumerId).orElse(null);
            if (consumer == null) {
                return ApiResponse.error("Consumer not found", "No consumer found with ID: " + consumerId);
            }

            // Get product
            Product product = productRepo.findById(productId).orElse(null);
            if (product == null) {
                return ApiResponse.error("Product not found", "No product found with ID: " + productId);
            }

            // Check stock
            if (product.getStock() < quantity) {
                return ApiResponse.error("Insufficient stock",
                        "Requested quantity (" + quantity + ") exceeds available stock (" + product.getStock() + ")");
            }

            // Find or create cart
            Order cart = orderRepo.findActiveCartByConsumerId(consumerId);
            if (cart == null) {
                cart = new Order();
                cart.setConsumer(consumer);
                cart.setOrderStatus(OrderStatus.CREATED);
                cart.setTotalAmount(0);
                cart.setOrderItems(new HashSet<>());
                cart = orderRepo.save(cart);
            }

            // Check if product already in cart
            OrderItem existingItem = null;
            for (OrderItem item : cart.getOrderItems()) {
                if (item.getProduct().getProductId() == productId) {
                    existingItem = item;
                    break;
                }
            }

            if (existingItem != null) {
                // Update existing item
                int newQuantity = existingItem.getQuantity() + quantity;

                // Recheck stock with new total
                if (product.getStock() < newQuantity) {
                    return ApiResponse.error("Insufficient stock",
                            "Cannot add " + quantity + " more units. Available: " + product.getStock() +
                                    ", already in cart: " + existingItem.getQuantity());
                }

                existingItem.setQuantity(newQuantity);
                existingItem.setUnitPrice(BigDecimal.valueOf(product.getPrice())
                        .multiply(BigDecimal.valueOf(newQuantity))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue());
                orderItemRepo.save(existingItem);
            } else {
                // Create new item
                OrderItem newItem = new OrderItem();
                newItem.setProduct(product);
                newItem.setQuantity(quantity);
                newItem.setUnitPrice(BigDecimal.valueOf(product.getPrice())
                        .multiply(BigDecimal.valueOf(quantity))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue());
                newItem.setOrder(cart);

                // Add to product's order items if needed
                if (product.getOrderItems() == null) {
                    product.setOrderItems(new HashSet<>());
                }
                product.getOrderItems().add(newItem);

                // Add to cart
                cart.getOrderItems().add(newItem);
                orderItemRepo.save(newItem);
            }

            // Update cart total
            cart.setTotalAmount(cart.getOrderItems().stream()
                    .mapToDouble(OrderItem::getUnitPrice)
                    .sum());
            orderRepo.save(cart);

            return ApiResponse.success("Item added to cart", cart.getOrderItems());
        } catch (Exception e) {
            logger.error("Failed to add to cart: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to add to cart", e.getMessage());
        }
    }

    /**
     * Remove from cart
     */
    @Transactional
    public ApiResponse<Set<OrderItem>> removeFromCart(int consumerId, int orderItemId, int quantity) {
        try {
            // Find order item in active cart
            OrderItem orderItem = orderItemRepo.findOrderItemWithStatusCREATED(consumerId, orderItemId);
            if (orderItem == null) {
                return ApiResponse.error("Item not found", "No item found in your active cart");
            }

            Order cart = orderItem.getOrder();

            // Remove item completely or reduce quantity
            if (quantity >= orderItem.getQuantity()) {
                // Remove entire item
                cart.getOrderItems().remove(orderItem);
                if (orderItem.getProduct() != null && orderItem.getProduct().getOrderItems() != null) {
                    orderItem.getProduct().getOrderItems().remove(orderItem);
                }
                orderItemRepo.delete(orderItem);
            } else {
                // Reduce quantity
                int newQuantity = orderItem.getQuantity() - quantity;
                double pricePerUnit = orderItem.getUnitPrice() / orderItem.getQuantity();
                orderItem.setQuantity(newQuantity);
                orderItem.setUnitPrice(BigDecimal.valueOf(pricePerUnit)
                        .multiply(BigDecimal.valueOf(newQuantity))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue());
                orderItemRepo.save(orderItem);
            }

            // Recalculate cart total
            cart.setTotalAmount(cart.getOrderItems().stream()
                    .mapToDouble(OrderItem::getUnitPrice)
                    .sum());
            orderRepo.save(cart);

            return ApiResponse.success(
                    quantity >= orderItem.getQuantity() ? "Item removed from cart" : "Item quantity reduced",
                    cart.getOrderItems());
        } catch (Exception e) {
            logger.error("Failed to remove from cart: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to remove from cart", e.getMessage());
        }
    }

    /**
     * Get cart
     */
    public ApiResponse<Order> getCart(int consumerId) {
        try {
            Order cart = orderRepo.findActiveCartByConsumerId(consumerId);
            if (cart == null) {
                return ApiResponse.success("Cart is empty", null);
            }
            return ApiResponse.success("Cart retrieved successfully", cart);
        } catch (Exception e) {
            logger.error("Failed to get cart: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to get cart", e.getMessage());
        }
    }

    /**
     * Clear change notifications
     */
    @Transactional
    public ApiResponse<Set<OrderItem>> acknowledgeChanges(int consumerId) {
        try {
            Order cart = orderRepo.findActiveCartByConsumerId(consumerId);
            if (cart == null || cart.getOrderItems().isEmpty()) {
                return ApiResponse.success("No items in cart", new HashSet<>());
            }

            for (OrderItem item : cart.getOrderItems()) {
                if (item.getFieldChange() != null) {
                    item.setFieldChange(null);
                    orderItemRepo.save(item);
                }
            }

            return ApiResponse.success("Changes acknowledged", cart.getOrderItems());
        } catch (Exception e) {
            logger.error("Failed to acknowledge changes: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to acknowledge changes", e.getMessage());
        }
    }

}
