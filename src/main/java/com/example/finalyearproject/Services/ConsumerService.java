package com.example.finalyearproject.Services;

import com.cloudinary.Cloudinary;
import com.example.finalyearproject.Abstraction.ConsumerRepo;
import com.example.finalyearproject.Abstraction.DeliveryAddressesRepo;
import com.example.finalyearproject.DataStore.Consumer;
import com.example.finalyearproject.DataStore.DeliveryAddresses;
import com.example.finalyearproject.Utility.ApiResponse;
import com.example.finalyearproject.Utility.ConsumerRegisterDTO;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;

@Service
public class ConsumerService {

    @Autowired
    private ConsumerRepo consumerRepo;

    @Autowired
    private DeliveryAddressesRepo deliveryAddressesRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Cloudinary cloudinary;

    private static final Logger logger = LoggerFactory.getLogger(ConsumerService.class);

    /**
     * Find a consumer by email
     */
    public Consumer findByEmail(String email) {
        try {
            return consumerRepo.findByConsumerEmail(email);
        } catch (Exception e) {
            logger.error("Error finding consumer by email: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get consumer profile by email
     */
    public ApiResponse<Consumer> getConsumerByEmail(String email) {
        try {
            Consumer consumer = consumerRepo.findByConsumerEmail(email);
            if (consumer == null) {
                return ApiResponse.error("Consumer not found", "No consumer found with email: " + email);
            }
            return ApiResponse.success("Consumer profile retrieved successfully", consumer);
        } catch (Exception e) {
            logger.error("Error retrieving consumer profile: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve consumer profile", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Consumer> RegisterConsumer(ConsumerRegisterDTO dto) {
        try {
            if (consumerRepo.findByConsumerEmail(dto.getConsumerEmail()) != null) {
                return ApiResponse.error("Registration failed", "Email already registered");
            }

            Consumer consumer = new Consumer();
            consumer.setConsumerFirstName(dto.getConsumerFirstName());
            consumer.setConsumerLastName(dto.getConsumerLastName());
            consumer.setConsumerEmail(dto.getConsumerEmail());
            consumer.setConsumerPhone(dto.getConsumerPhone());
            consumer.setConsumerAddress(dto.getConsumerAddress());
            consumer.setConsumerPassword(passwordEncoder.encode(dto.getConsumerPassword()));

            // Handle profile image upload
            if (dto.getProfilePhoto() != null && !dto.getProfilePhoto().isEmpty()) {
                try {
                    String imagePath = uploadConsumerProfilePhoto(dto.getProfilePhoto(), dto.getConsumerEmail());
                    consumer.setProfilePhotoPath(imagePath);
                } catch (IOException e) {
                    logger.error("Failed to upload profile photo: {}", e.getMessage());
                    // Continue with registration without the profile photo
                }
            }

            Consumer saved = consumerRepo.save(consumer);
            return ApiResponse.success("Consumer registered successfully", saved);
        } catch (Exception e) {
            logger.error("Failed to register consumer: {}", e.getMessage(), e);
            return ApiResponse.error("Registration failed", e.getMessage());
        }
    }

    public String uploadConsumerProfilePhoto(MultipartFile file, String email) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile photo is empty or missing");
        }

        // File type validation
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/"))) {
            throw new IllegalArgumentException("Uploaded file is not an image");
        }

        // Size validation (e.g., 5MB max)
        long maxSizeBytes = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Image size exceeds maximum allowed (5MB)");
        }

        try {
            // Create a folder path for organizing images in Cloudinary
            String folderPath = "profiles/consumers/" + email.replaceAll("[^a-zA-Z0-9.\\-]", "_");

            // Prepare upload parameters
            Map<String, Object> params = new HashMap<>();
            params.put("folder", folderPath);
            params.put("public_id", UUID.randomUUID().toString());
            params.put("overwrite", true);
            params.put("resource_type", "auto");

            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            // Validate return value
            if (uploadResult == null || !uploadResult.containsKey("secure_url")) {
                throw new IOException("Invalid response from image upload service");
            }

            // Return the secure URL of the uploaded image
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            logger.error("Failed to upload image to Cloudinary: {}", e.getMessage(), e);
            throw new IOException("Failed to upload image: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.error("Cloudinary upload error: {}", e.getMessage(), e);
            throw new IOException("Image upload service failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update consumer profile photo
     */
    @Transactional
    public ApiResponse<String> updateProfilePhoto(MultipartFile file, String email) {
        try {
            Consumer consumer = consumerRepo.findByConsumerEmail(email);
            if (consumer == null) {
                return ApiResponse.error("Update failed", "Consumer not found with email: " + email);
            }

            // If consumer has an existing photo and it's a Cloudinary URL, try to delete it
            String existingPhotoPath = consumer.getProfilePhotoPath();
            if (existingPhotoPath != null && existingPhotoPath.contains("cloudinary.com")) {
                try {
                    // Extract public_id from URL - this may need adjustment based on your URL format
                    String publicId = existingPhotoPath.substring(
                            existingPhotoPath.lastIndexOf("/") + 1,
                            existingPhotoPath.lastIndexOf(".")
                    );
                    // Delete from Cloudinary
                    Map<String, String> params = new HashMap<>();
                    params.put("resource_type", "image");
                    cloudinary.uploader().destroy(publicId, params);
                } catch (Exception e) {
                    logger.warn("Failed to delete existing profile photo: {}", e.getMessage());
                    // Continue with the upload even if deletion fails
                }
            }

            // Upload new photo
            String newPhotoUrl = uploadConsumerProfilePhoto(file, email);

            // Update consumer record
            consumer.setProfilePhotoPath(newPhotoUrl);
            consumerRepo.save(consumer);

            return ApiResponse.success("Profile photo updated successfully", newPhotoUrl);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid file: {}", e.getMessage());
            return ApiResponse.error("Update failed", e.getMessage());
        } catch (IOException e) {
            logger.error("Failed to upload profile photo: {}", e.getMessage(), e);
            return ApiResponse.error("Update failed", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error updating profile photo: {}", e.getMessage(), e);
            return ApiResponse.error("Update failed", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Consumer> UpdateConsumer(Consumer consumer) {
        try {
            if (consumer == null || consumer.getConsumerId() == 0) {
                return ApiResponse.error("Update failed", "Invalid consumer data");
            }

            Consumer existingConsumer = consumerRepo.findConsumerByConsumerId(consumer.getConsumerId());
            if (existingConsumer == null) {
                return ApiResponse.error("Update failed", "Consumer not found with ID: " + consumer.getConsumerId());
            }

            // Update only the allowed fields, preserving sensitive information
            existingConsumer.setConsumerFirstName(consumer.getConsumerFirstName());
            existingConsumer.setConsumerLastName(consumer.getConsumerLastName());
            existingConsumer.setConsumerPhone(consumer.getConsumerPhone());
            existingConsumer.setConsumerAddress(consumer.getConsumerAddress());

            // Only update password if provided and not empty
            if (consumer.getPassword() != null && !consumer.getPassword().isEmpty()) {
                existingConsumer.setConsumerPassword(passwordEncoder.encode(consumer.getPassword()));
            }

            Consumer updatedConsumer = consumerRepo.save(existingConsumer);
            return ApiResponse.success("Consumer updated successfully", updatedConsumer);
        } catch (Exception e) {
            logger.error("Failed to update consumer: {}", e.getMessage(), e);
            return ApiResponse.error("Update failed", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Set<DeliveryAddresses>> AddDeliveryAddress(DeliveryAddresses deliveryAddress, int consumerId) {
        try {
            if (deliveryAddress == null) {
                return ApiResponse.error("Invalid request", "Delivery address cannot be null");
            }

            Consumer consumer = consumerRepo.findConsumerByConsumerId(consumerId);
            if (consumer == null) {
                return ApiResponse.error("Consumer not found", "No consumer found with ID: " + consumerId);
            }

            deliveryAddress.setConsumer(consumer);
            if (consumer.getSetOfDeliveryAddress() == null) {
                consumer.setSetOfDeliveryAddress(new HashSet<>());
            }

            consumer.getSetOfDeliveryAddress().add(deliveryAddress);
            consumerRepo.save(consumer);

            return ApiResponse.success("Delivery address added successfully", consumer.getSetOfDeliveryAddress());
        } catch (Exception e) {
            logger.error("Failed to add delivery address: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to add delivery address", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<DeliveryAddresses> UpdateDeliveryAddress(DeliveryAddresses address, int consumerId, int addressId) {
        try {
            if (address == null) {
                return ApiResponse.error("Invalid request", "Delivery address cannot be null");
            }

            Consumer consumer = consumerRepo.findConsumerByConsumerId(consumerId);
            if (consumer == null) {
                return ApiResponse.error("Consumer not found", "No consumer found with ID: " + consumerId);
            }

            // Verify the address belongs to this consumer
            boolean addressBelongsToConsumer = consumer.getSetOfDeliveryAddress().stream()
                    .anyMatch(addr -> addr.getDeliveryAddressId() == addressId);

            if (!addressBelongsToConsumer) {
                return ApiResponse.error("Unauthorized", "Address does not belong to this consumer");
            }

            consumerRepo.updateDeliveryAddress(address, addressId, consumerId);
            DeliveryAddresses updatedAddress = deliveryAddressesRepo.findDeliveryAddressesByDeliveryAddressId(addressId);

            return ApiResponse.success("Delivery address updated successfully", updatedAddress);
        } catch (Exception e) {
            logger.error("Failed to update delivery address: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to update delivery address", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<Set<DeliveryAddresses>> DeleteDeliveryAddress(int addressId, int consumerId) {
        try {
            Consumer consumer = consumerRepo.findConsumerByConsumerId(consumerId);
            if (consumer == null) {
                return ApiResponse.error("Consumer not found", "No consumer found with ID: " + consumerId);
            }

            // Verify the address belongs to this consumer
            boolean addressBelongsToConsumer = consumer.getSetOfDeliveryAddress().stream()
                    .anyMatch(addr -> addr.getDeliveryAddressId() == addressId);

            if (!addressBelongsToConsumer) {
                return ApiResponse.error("Unauthorized", "Address does not belong to this consumer");
            }

            consumerRepo.deleteDeliveryAddressById(addressId, consumerId);
            Set<DeliveryAddresses> remainingAddresses = consumer.getSetOfDeliveryAddress();

            return ApiResponse.success("Delivery address deleted successfully", remainingAddresses);
        } catch (Exception e) {
            logger.error("Failed to delete delivery address: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to delete delivery address", e.getMessage());
        }
    }

    /**
     * Get all delivery addresses for a consumer
     */
    public ApiResponse<Set<DeliveryAddresses>> getDeliveryAddresses(int consumerId) {
        try {
            Consumer consumer = consumerRepo.findConsumerByConsumerId(consumerId);
            if (consumer == null) {
                return ApiResponse.error("Consumer not found", "No consumer found with ID: " + consumerId);
            }

            Set<DeliveryAddresses> addresses = consumer.getSetOfDeliveryAddress();
            if (addresses == null) {
                addresses = new HashSet<>();
            }

            return ApiResponse.success("Delivery addresses retrieved successfully", addresses);
        } catch (Exception e) {
            logger.error("Failed to get delivery addresses: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve delivery addresses", e.getMessage());
        }
    }
}