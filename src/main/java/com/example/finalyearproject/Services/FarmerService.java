package com.example.finalyearproject.Services;

import com.example.finalyearproject.Abstraction.FarmerRepo;
import com.example.finalyearproject.DataStore.Farmer;
import com.example.finalyearproject.Utility.ApiResponse;
import com.example.finalyearproject.Utility.FarmerRegisterDTO;
import com.cloudinary.Cloudinary;
import com.example.finalyearproject.Utility.FarmerUpdateDTO;
import com.example.finalyearproject.Utility.FarmerUtility;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class FarmerService {

    private static final Logger logger = LoggerFactory.getLogger(FarmerService.class);

    @Autowired
    private FarmerRepo farmerRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Cloudinary cloudinary;

    @Transactional
    public ApiResponse<Farmer> RegisterFarmer(FarmerRegisterDTO dto) {
        try {
            // Check for existing email or phone
            if (farmerRepo.findByFarmerEmail(dto.getFarmerEmail()) != null) {
                return ApiResponse.error("Registration failed", "Email already registered");
            }

            Farmer farmer = new Farmer();
            farmer.setFarmerEmail(dto.getFarmerEmail());
            farmer.setFirstName(dto.getFirstName());
            farmer.setLastName(dto.getLastName());
            farmer.setFarmerPassword(passwordEncoder.encode(dto.getFarmerPassword()));
            farmer.setFarmerPhone(dto.getFarmerPhone());
            farmer.setFarmerAddress(dto.getFarmerAddress());

            // Upload profile photo if provided
            if (dto.getProfilePhoto() != null && !dto.getProfilePhoto().isEmpty()) {
                try {
                    String imageUrl = uploadProfilePhoto(dto.getProfilePhoto(), dto.getFarmerEmail());
                    farmer.setProfilePhotoPath(imageUrl);
                } catch (IOException e) {
                    logger.error("Failed to upload profile photo: {}", e.getMessage());
                    // Continue registration process without profile photo
                }
            }

            Farmer saved = farmerRepo.save(farmer);
            return ApiResponse.success("Farmer registered successfully", saved);

        } catch (Exception e) {
            logger.error("Failed to register farmer: {}", e.getMessage(), e);
            return ApiResponse.error("Registration failed", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<FarmerUtility> updateFarmer(FarmerUpdateDTO updateDTO, String farmerEmail) {
        try {
            Farmer existingFarmer = farmerRepo.findByFarmerEmail(farmerEmail);
            if (existingFarmer == null) {
                return ApiResponse.error("Update failed", "Farmer not found with email: " + farmerEmail);
            }

            // Update only the fields provided in the DTO
            if (updateDTO.getFirstName() != null && !updateDTO.getFirstName().isEmpty()) {
                existingFarmer.setFirstName(updateDTO.getFirstName());
            }

            if (updateDTO.getLastName() != null && !updateDTO.getLastName().isEmpty()) {
                existingFarmer.setLastName(updateDTO.getLastName());
            }

            if (updateDTO.getFarmerPhone() != null && !updateDTO.getFarmerPhone().isEmpty()) {
                existingFarmer.setFarmerPhone(updateDTO.getFarmerPhone());
            }

            if (updateDTO.getFarmerAddress() != null && !updateDTO.getFarmerAddress().isEmpty()) {
                existingFarmer.setFarmerAddress(updateDTO.getFarmerAddress());
            }

            // Only update password if provided
//            if (updateDTO.getPassword() != null && !updateDTO.getPassword().isEmpty()) {
//                existingFarmer.setFarmerPassword(passwordEncoder.encode(updateDTO.getPassword()));
//            }

            // Save the updated farmer
            Farmer updatedAndSavedFarmer = farmerRepo.save(existingFarmer);
            FarmerUtility updatedFarmer=new FarmerUtility(
                    updatedAndSavedFarmer.getFirstName(),
                    updatedAndSavedFarmer.getLastName(),
                    updatedAndSavedFarmer.getFarmerPhone(),
                    updatedAndSavedFarmer.getFarmerAddress());
            return ApiResponse.success("Farmer updated successfully", updatedFarmer);
        } catch (Exception e) {
            logger.error("Failed to update farmer: {}", e.getMessage(), e);
            return ApiResponse.error("Update failed", e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<String> updateProfilePhoto(MultipartFile file, String farmerEmail) {
        try {
            Farmer farmer = farmerRepo.findByFarmerEmail(farmerEmail);
            if (farmer == null) {
                return ApiResponse.error("Update failed", "Farmer not found with email: " + farmerEmail);
            }

            // Delete existing photo logic...

            String newPhotoUrl = uploadProfilePhoto(file, farmerEmail);
            farmer.setProfilePhotoPath(newPhotoUrl);
            farmerRepo.save(farmer);

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

    public String uploadProfilePhoto(MultipartFile file, String email) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile photo is empty or missing");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        try {
            // Create a folder path for organizing images in Cloudinary
            String folderPath = "profiles/farmers/" + email.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

            // Prepare upload parameters
            Map<String, Object> params = new HashMap<>();
            params.put("folder", folderPath);
            params.put("public_id", UUID.randomUUID().toString());
            params.put("overwrite", true);
            params.put("resource_type", "auto");

            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            // Return the secure URL of the uploaded image
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            logger.error("Failed to upload image to Cloudinary: {}", e.getMessage(), e);
            throw new IOException("Failed to upload image: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.error("Cloudinary service error: {}", e.getMessage(), e);
            throw new IOException("Cloud storage service error: " + e.getMessage(), e);
        }
    }

}