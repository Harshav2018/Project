package com.example.finalyearproject.Services;

import com.cloudinary.Cloudinary;
import com.example.finalyearproject.Abstraction.ProductImageRepository;
import com.example.finalyearproject.Abstraction.ProductRepo;
import com.example.finalyearproject.DataStore.Product;
import com.example.finalyearproject.DataStore.ProductImage;
import com.example.finalyearproject.Utility.ApiResponse;
import com.example.finalyearproject.customExceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.*;

@Service
@Transactional
public class ProductImageService {

    private static final Logger logger = LoggerFactory.getLogger(ProductImageService.class);

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private Cloudinary cloudinary;

    public ApiResponse<List<ProductImage>> uploadProductImages(int productId, MultipartFile[] files) {
        List<String> successfullyUploadedIds = new ArrayList<>();
        List<ProductImage> uploadedImages = new ArrayList<>();

        try {
            Product product = productRepo.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));

            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                // Validate image before upload
                if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                    logger.warn("Skipping non-image file for product {}: {}", productId, file.getOriginalFilename());
                    continue;
                }

                try {
                    // Prepare upload parameters for Cloudinary
                    String folderPath = "products/" + productId;
                    Map<String, Object> params = new HashMap<>();
                    params.put("folder", folderPath);
                    params.put("public_id", UUID.randomUUID().toString());
                    params.put("overwrite", true);
                    params.put("resource_type", "auto");

                    // Upload to Cloudinary
                    Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

                    // Get the secure URL and public ID
                    String secureUrl = uploadResult.get("secure_url").toString();
                    String publicId = uploadResult.get("public_id").toString();

                    successfullyUploadedIds.add(publicId);

                    // Create and save a ProductImage entity
                    ProductImage image = new ProductImage();
                    image.setFilename(publicId);
                    image.setFilePath(secureUrl);
                    image.setProduct(product);

                    // Add image to product images set
                    product.getImages().add(image);
                    ProductImage savedImage = productImageRepository.save(image);
                    uploadedImages.add(savedImage);

                    logger.info("Successfully uploaded image for product {}: {}", productId, publicId);

                } catch (IOException e) {
                    logger.error("Failed to upload image to Cloudinary for product {}: {}",
                            productId, e.getMessage(), e);
                    // Continue with next image rather than failing entire batch
                }
            }

            // Save the updated product with its new images
            productRepo.save(product);

            if (uploadedImages.isEmpty()) {
                return ApiResponse.error("No images uploaded", "No valid images were provided or all uploads failed");
            }

            return ApiResponse.success("Images uploaded successfully", uploadedImages);

        } catch (ResourceNotFoundException e) {
            logger.error("Product not found for image upload: {}", e.getMessage());
            // Attempt to rollback any uploads if product not found
            rollbackCloudinaryUploads(successfullyUploadedIds);
            return ApiResponse.error("Upload failed", e.getMessage());
        } catch (Exception e) {
            logger.error("Error during product image upload process: {}", e.getMessage(), e);
            // Rollback any successful uploads
            rollbackCloudinaryUploads(successfullyUploadedIds);
            return ApiResponse.error("Upload failed", e.getMessage());
        }
    }

    private void rollbackCloudinaryUploads(List<String> publicIds) {
        for (String publicId : publicIds) {
            try {
                Map<String, String> params = new HashMap<>();
                params.put("resource_type", "image");
                cloudinary.uploader().destroy(publicId, params);
                logger.info("Rolled back image upload: {}", publicId);
            } catch (Exception e) {
                logger.warn("Failed to roll back image upload for {}: {}", publicId, e.getMessage());
                // Continue with other deletions even if one fails
            }
        }
    }

    public ApiResponse<Void> deleteAllImagesForProduct(int productId) {
        try {
            Product product = productRepo.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));

            boolean hasFailedDeletions = false;
            List<String> failedIds = new ArrayList<>();

            // Delete each image from Cloudinary only
            for (ProductImage image : product.getImages()) {
                try {
                    // Extract public_id from the filename field
                    String publicId = image.getFilename();

                    // Delete from Cloudinary
                    Map<String, String> params = new HashMap<>();
                    params.put("resource_type", "image");
                    cloudinary.uploader().destroy(publicId, params);
                    logger.info("Successfully deleted image {} for product {}", publicId, productId);

                } catch (IOException e) {
                    hasFailedDeletions = true;
                    failedIds.add(image.getFilename());
                    logger.error("Failed to delete image {} for product {}: {}",
                            image.getFilename(), productId, e.getMessage(), e);
                } catch (RuntimeException e) {
                    hasFailedDeletions = true;
                    failedIds.add(image.getFilename());
                    logger.error("Cloud service error while deleting image {} for product {}: {}",
                            image.getFilename(), productId, e.getMessage(), e);
                }
            }

            // Log warning if some deletions failed
            if (hasFailedDeletions) {
                logger.warn("Some images could not be deleted from cloud storage for product {}: {}",
                        productId, String.join(", ", failedIds));
                return ApiResponse.success("Product images partially deleted - some cloud resources could not be removed");
            }

            return ApiResponse.success("All product images deleted successfully");

        } catch (ResourceNotFoundException e) {
            logger.error("Product not found for image deletion: {}", e.getMessage());
            return ApiResponse.error("Deletion failed", e.getMessage());
        } catch (Exception e) {
            logger.error("Error during product image deletion: {}", e.getMessage(), e);
            return ApiResponse.error("Deletion failed", e.getMessage());
        }
    }

    /**
     * Delete a single product image
     */
    public ApiResponse<Void> deleteProductImage(int imageId) {
        try {
            ProductImage image = productImageRepository.findById(imageId)
                    .orElseThrow(() -> new ResourceNotFoundException("Image not found with id " + imageId));

            try {
                // Delete from Cloudinary
                String publicId = image.getFilename();
                Map<String, String> params = new HashMap<>();
                params.put("resource_type", "image");
                cloudinary.uploader().destroy(publicId, params);

                // Remove from product's image collection
                Product product = image.getProduct();
                product.getImages().remove(image);

                // Delete from database
                productImageRepository.delete(image);

                logger.info("Successfully deleted image {} (id: {})", publicId, imageId);

                return ApiResponse.success("Image deleted successfully");

            } catch (IOException e) {
                logger.error("Failed to delete image {} from cloud storage: {}",
                        image.getFilename(), e.getMessage(), e);
                return ApiResponse.error("Deletion failed", "Failed to delete image from cloud storage: " + e.getMessage());
            } catch (RuntimeException e) {
                logger.error("Cloud service error while deleting image {}: {}",
                        image.getFilename(), e.getMessage(), e);
                return ApiResponse.error("Deletion failed", "Cloud storage service error: " + e.getMessage());
            }
        } catch (ResourceNotFoundException e) {
            logger.error("Image not found for deletion: {}", e.getMessage());
            return ApiResponse.error("Deletion failed", e.getMessage());
        } catch (Exception e) {
            logger.error("Error during image deletion: {}", e.getMessage(), e);
            return ApiResponse.error("Deletion failed", e.getMessage());
        }
    }

    /**
     * Verify that an image belongs to a product owned by the specified farmer
     */
    public ApiResponse<Boolean> verifyImageOwnership(int imageId, String farmerEmail) {
        try {
            Optional<ProductImage> imageOpt = productImageRepository.findById(imageId);
            if (imageOpt.isEmpty()) {
                return ApiResponse.error("Image not found", "No image found with ID: " + imageId);
            }

            ProductImage image = imageOpt.get();
            Product product = image.getProduct();

            if (product == null) {
                return ApiResponse.error("Invalid image", "Image is not associated with any product");
            }

            boolean isOwner = product.getFarmer().getFarmerEmail().equals(farmerEmail);
            return ApiResponse.success(isOwner ? "Image belongs to farmer" : "Image does not belong to farmer", isOwner);
        } catch (Exception e) {
            logger.error("Failed to verify image ownership: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to verify ownership", e.getMessage());
        }
    }
}
