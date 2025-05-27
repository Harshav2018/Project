package com.example.finalyearproject.Services;

import com.example.finalyearproject.Abstraction.FarmerRepo;
import com.example.finalyearproject.Abstraction.OrderItemRepo;
import com.example.finalyearproject.Abstraction.OrderRepo;
import com.example.finalyearproject.Abstraction.ProductRepo;
import com.example.finalyearproject.DataStore.*;
import com.example.finalyearproject.Specifications.ProductSpecification;
import com.example.finalyearproject.Utility.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private FarmerRepo farmerRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private OrderItemRepo orderItemRepo;

    @Autowired
    private ProductSessionManager sessionManager;

    @Autowired
    private ProductImageService productImageService;

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Transactional
    public ApiResponse<Product> AddProduct(ProductUtility prodUtil, String farmerEmail) {
        try {
            Product product = new Product();
            product.setName(prodUtil.getName());
            product.setDescription(prodUtil.getDescription());
            product.setPrice(prodUtil.getPrice());
            product.setStock(prodUtil.getStock());
            product.setHarvestDate(prodUtil.getHarvestDate());
            product.setAvailableFromDate(prodUtil.getAvailableFromDate());
            product.setOrganic(prodUtil.isOrganic());

            try {
                CategoryType category = CategoryType.valueOf(prodUtil.getCategory().toUpperCase());
                product.setCategory(category);
            } catch (IllegalArgumentException e) {
                return ApiResponse.error("Product creation failed", "Invalid category: " + prodUtil.getCategory());
            }

            Farmer farmer = farmerRepo.findByFarmerEmail(farmerEmail);
            if (farmer == null) {
                return ApiResponse.error("Product creation failed", "Farmer not found with email: " + farmerEmail);
            }

            product.setFarmer(farmer);
            farmer.getFarmerProducts().add(product);
            Product storedProduct = productRepo.save(product);

            // Handle image uploads
            if (prodUtil.getImages() != null && prodUtil.getImages().length > 0) {
                try {
                    productImageService.uploadProductImages(storedProduct.getProductId(), prodUtil.getImages());
                } catch (Exception e) {
                    logger.error("Failed to upload product images: {}", e.getMessage(), e);
                    // Continue with product creation even if image upload fails
                }
            }

            return ApiResponse.success("Product added successfully", storedProduct);
        } catch (Exception e) {
            logger.error("Failed to add product: {}", e.getMessage(), e);
            return ApiResponse.error("Product creation failed", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Product> updateProduct(ProductUpdateDTO dto, int productId, String farmerEmail) {
        try {
            // Retrieve the existing product
            Product existingProduct = productRepo.findProductByProductId(productId);
            if (existingProduct == null) {
                return ApiResponse.error("Update failed", "Product not found with ID: " + productId);
            }

            // Retrieve the authenticated farmer
            Farmer farmer = farmerRepo.findByFarmerEmail(farmerEmail);
            if (farmer == null) {
                return ApiResponse.error("Update failed", "Farmer not found with email: " + farmerEmail);
            }

            // Verify product ownership
            if (!existingProduct.getFarmer().getFarmerEmail().equalsIgnoreCase(farmerEmail)) {
                return ApiResponse.error("Update failed", "You don't have permission to update this product");
            }

            // Save old values for later order adjustment
            double oldPrice = existingProduct.getPrice();
            int oldStock = existingProduct.getStock();

            // Update product fields
            existingProduct.setName(dto.getName());
            existingProduct.setDescription(dto.getDescription());
            existingProduct.setPrice(dto.getPrice());
            existingProduct.setStock(dto.getStock());
            existingProduct.setCategory(dto.getCategory());
            existingProduct.setHarvestDate(dto.getHarvestDate());
            existingProduct.setAvailableFromDate(dto.getAvailableFromDate());
            existingProduct.setOrganic(dto.isOrganic());
            existingProduct.setImages(existingProduct.getImages());

            productRepo.save(existingProduct);

            // Adjust associated order items (only for orders with status "CREATED")
            if (existingProduct.getOrderItems() != null && !existingProduct.getOrderItems().isEmpty()) {
                for (OrderItem item : existingProduct.getOrderItems()) {
                    Order order = item.getOrder();
                    if (!"CREATED".equals(order.getOrderStatus().toString())) {
                        continue;
                    }

                    StringBuilder changeMsg = new StringBuilder();

                    // Update price if changed
                    if (oldPrice != dto.getPrice()) {
                        double delta = (dto.getPrice() - oldPrice) * item.getQuantity();
                        item.setUnitPrice(item.getUnitPrice() + delta);
                        order.setTotalAmount(order.getTotalAmount() + delta);
                        changeMsg.append(oldPrice < dto.getPrice() ? "Price Increased" : "Price Decreased");
                    }

                    // Update stock if changed and current order quantity exceeds the new stock
                    if (oldStock != dto.getStock() && item.getQuantity() > dto.getStock()) {
                        int removedQty = item.getQuantity() - dto.getStock();
                        double deduct = removedQty * dto.getPrice();
                        item.setQuantity(dto.getStock());
                        item.setUnitPrice(item.getUnitPrice() - deduct);
                        order.setTotalAmount(order.getTotalAmount() - deduct);
                        changeMsg.append(" | Stock Reduced");
                    }

                    item.setFieldChange(changeMsg.toString());
                    orderItemRepo.save(item);
                    orderRepo.save(order);
                }
            }

            // Return updated product
            Product updatedProduct = productRepo.findProductByProductId(productId);
            return ApiResponse.success("Product updated successfully", updatedProduct);
        } catch (Exception e) {
            logger.error("Failed to update product: {}", e.getMessage(), e);
            return ApiResponse.error("Update failed", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Void> DeleteProduct(int productId, String farmerEmail) {
        try {
            Farmer byFarmerEmail = farmerRepo.findByFarmerEmail(farmerEmail);
            if (byFarmerEmail == null) {
                return ApiResponse.error("Deletion failed", "Farmer not found with email: " + farmerEmail);
            }

            // Find the product
            Optional<Product> productOpt = productRepo.findByFarmer_FarmerIdAndProductId(productId, byFarmerEmail.getFarmerId());
            if (productOpt.isEmpty()) {
                return ApiResponse.error("Deletion failed", "Product not found with ID " + productId +
                        " for farmer " + byFarmerEmail.getFarmerName());
            }

            // Handle image deletion - check the response instead of catching an exception
            ApiResponse<Void> imageDeleteResponse = productImageService.deleteAllImagesForProduct(productId);
            if (imageDeleteResponse.getErrors() != null) {
                // Log that image deletion had issues but continue with product deletion
                logger.warn("Issue with image deletion: {}", imageDeleteResponse.getMessage());
                // Continuing with product deletion despite image deletion issues
            }

            // Delete the product
            productRepo.delete(productOpt.get());
            return ApiResponse.success("Product deleted successfully");
        } catch (Exception e) {
            logger.error("Failed to delete product: {}", e.getMessage(), e);
            return ApiResponse.error("Deletion failed", e.getMessage());
        }
    }
    /**
     * Get all products
     */
    public ApiResponse<List<Product>> getAllProducts() {
        try {
            List<Product> products = productRepo.findAll();
            return ApiResponse.success("Products retrieved successfully", products);
        } catch (Exception e) {
            logger.error("Failed to get all products: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve products", e.getMessage());
        }
    }

    /**
     * Get a product by ID
     */
    public ApiResponse<Product> getProductById(int productId) {
        try {
            Optional<Product> productOpt = productRepo.findById(productId);
            return productOpt.map(product -> ApiResponse.success("Product retrieved successfully", product)).orElseGet(() -> ApiResponse.error("Product not found", "No product found with ID: " + productId));
        } catch (Exception e) {
            logger.error("Failed to get product by ID: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve product", e.getMessage());
        }
    }

    /**
     * Get products by category
     */
    public ApiResponse<List<Product>> getProductsByCategory(String categoryStr) {
        try {
            CategoryType category;
            try {
                category = CategoryType.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ApiResponse.error("Invalid category", "Category not found: " + categoryStr);
            }

            List<Product> products = productRepo.findByCategory(category);
            return ApiResponse.success("Products retrieved successfully", products);
        } catch (Exception e) {
            logger.error("Failed to get products by category: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve products", e.getMessage());
        }
    }

    /**
     * Get products by farmer ID
     */
    public ApiResponse<List<Product>> getProductsByFarmerId(int farmerId) {
        try {
            Optional<Farmer> farmerOpt = farmerRepo.findByFarmerId(farmerId);
            if (farmerOpt.isEmpty()) {
                return ApiResponse.error("Farmer not found", "No farmer found with ID: " + farmerId);
            }

            List<Product> products = productRepo.findByFarmer_FarmerId(farmerId);
            return ApiResponse.success("Products retrieved successfully", products);
        } catch (Exception e) {
            logger.error("Failed to get products by farmer ID: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve products", e.getMessage());
        }
    }

    /**
     * Get products by farmer email
     */
    public ApiResponse<List<Product>> getProductsByFarmerEmail(String farmerEmail) {
        try {
            Farmer farmer = farmerRepo.findByFarmerEmail(farmerEmail);
            if (farmer == null) {
                return ApiResponse.error("Farmer not found", "No farmer found with email: " + farmerEmail);
            }

            List<Product> products = productRepo.findByFarmer_FarmerId(farmer.getFarmerId());
            return ApiResponse.success("Products retrieved successfully", products);
        } catch (Exception e) {
            logger.error("Failed to get products by farmer email: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve products", e.getMessage());
        }
    }

    /**
     * Get a product by ID and verify it belongs to a specific farmer
     */
    public ApiResponse<Product> getProductByIdAndFarmerEmail(int productId, String farmerEmail) {
        try {
            Product product = productRepo.findProductByProductId(productId);
            if (product == null) {
                return ApiResponse.error("Product not found", "No product found with ID: " + productId);
            }

            if (!product.getFarmer().getFarmerEmail().equals(farmerEmail)) {
                return ApiResponse.error("Unauthorized", "This product does not belong to the authenticated farmer");
            }

            return ApiResponse.success("Product retrieved successfully", product);
        } catch (Exception e) {
            logger.error("Failed to get product by ID and farmer email: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve product", e.getMessage());
        }
    }

    /**
     * Search products by name
     */
    public ApiResponse<List<Product>> searchProductsByName(String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ApiResponse.error("Invalid search", "Search query cannot be empty");
            }

            List<Product> products = productRepo.findByNameContainingIgnoreCase(query);
            return ApiResponse.success("Search results retrieved successfully", products);
        } catch (Exception e) {
            logger.error("Failed to search products: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to search products", e.getMessage());
        }
    }

    /**
     * Get featured products
     * This is a placeholder - implement your business logic for determining featured products
     */
    public ApiResponse<List<Product>> getFeaturedProducts() {
        try {
            // This is just an example implementation
            // You might want to implement different logic based on your requirements
            // For example, products with highest ratings, most ordered, etc.
            List<Product> products = productRepo.findTop10ByOrderByAverageRatingDesc();
            return ApiResponse.success("Featured products retrieved successfully", products);
        } catch (Exception e) {
            logger.error("Failed to get featured products: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve featured products", e.getMessage());
        }
    }

    /**
     * Get recently added products
     */
    public ApiResponse<List<Product>> getRecentProducts() {
        try {
            // Assuming you have a createdAt field or similar to sort by
            // If not, you'll need to adjust this method or add such a field to your Product entity
            List<Product> products = productRepo.findTop10ByOrderByProductIdDesc(); // Using ID as a proxy for recency
            return ApiResponse.success("Recent products retrieved successfully", products);
        } catch (Exception e) {
            logger.error("Failed to get recent products: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve recent products", e.getMessage());
        }
    }

    public ApiResponse<Page<Product>> getFilteredProducts(ProductFilterDTO filterDTO) {
        try {
            // Create the specification from filter
            Specification<Product> spec = ProductSpecification.getFilteredProducts(filterDTO);

            // Create sort based on the sortBy parameter
            Sort sort = Sort.unsorted();
            if (filterDTO.getSortBy() != null) {
                sort = switch (filterDTO.getSortBy()) {
                    case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
                    case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
                    case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
                    case "name_desc" -> Sort.by(Sort.Direction.DESC, "name");
                    case "date_asc" -> Sort.by(Sort.Direction.ASC, "availableFromDate");
                    case "date_desc" -> Sort.by(Sort.Direction.DESC, "availableFromDate");
                    default ->
                        // Default sort
                            Sort.by(Sort.Direction.DESC, "productId");
                };
            }

            // Create pageable with sort and pagination
            Pageable pageable = PageRequest.of(
                    filterDTO.getPage(),
                    filterDTO.getSize(),
                    sort
            );

            // Fetch filtered products
            Page<Product> products = productRepo.findAll(spec, pageable);

            return ApiResponse.success("Products retrieved successfully", products);
        } catch (Exception e) {
            logger.error("Failed to filter products: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to filter products", e.getMessage());
        }
    }

    public ApiResponse<Page<Product>> getRandomProductsPaginated(Pageable pageable) {
        try {
            // Initialize shuffled product IDs if not done already
            if (!sessionManager.hasShuffledIds()) {
                List<Integer> allProductIds = productRepo.findAllAvailableProductIds();
                sessionManager.setShuffledProductIds(allProductIds);
            }

            // Get the specific page of product IDs
            List<Integer> pageProductIds = sessionManager.getPageOfIds(
                    pageable.getPageNumber(),
                    pageable.getPageSize());

            if (pageProductIds.isEmpty()) {
                return ApiResponse.success(
                        "No more products available",
                        new PageImpl<>(Collections.emptyList(), pageable, sessionManager.getShuffledProductIds().size())
                );
            }

            // Fetch the actual products in the order of the IDs
            List<Product> products = productRepo.findByProductIdsInOrderNative(pageProductIds);

            // Create a Page object
            Page<Product> productPage = new PageImpl<>(
                    products,
                    pageable,
                    sessionManager.getShuffledProductIds().size()
            );

            return ApiResponse.success("Random products retrieved successfully", productPage);
        } catch (Exception e) {
            logger.error("Failed to get random products: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve random products", e.getMessage());
        }
    }

    /**
     * Reset the random order (can be called when a user explicitly wants a new shuffle)
     */
    public ApiResponse<String> resetRandomOrder() {
        try {
            List<Integer> allProductIds = productRepo.findAllAvailableProductIds();
            sessionManager.setShuffledProductIds(allProductIds);
            return ApiResponse.success("Random product order has been reset", null);
        } catch (Exception e) {
            logger.error("Failed to reset random order: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to reset random order", e.getMessage());
        }
    }

}