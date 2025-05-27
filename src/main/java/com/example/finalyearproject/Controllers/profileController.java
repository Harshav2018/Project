package com.example.finalyearproject.Controllers;

import com.example.finalyearproject.DataStore.Consumer;
import com.example.finalyearproject.DataStore.DeliveryAddresses;
import com.example.finalyearproject.Services.ConsumerService;
import com.example.finalyearproject.Utility.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/profile")
public class profileController {

    @Autowired
    private ConsumerService consumerService;

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<Consumer>> updateConsumer(
            @RequestBody Consumer consumer,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Consumer currentConsumer = consumerService.findByEmail(email);

            if (currentConsumer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Update failed", "Consumer not found"));
            }

            // Only update allowed fields, keep ID the same
            consumer.setConsumerId(currentConsumer.getConsumerId());

            ApiResponse<Consumer> response = consumerService.UpdateConsumer(consumer);

            if (response.getData() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Update failed", e.getMessage()));
        }
    }

    @PostMapping("/profile-photo")
    public ResponseEntity<ApiResponse<String>> updateProfilePhoto(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            ApiResponse<String> response = consumerService.updateProfilePhoto(file, email);

            if (response.getData() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Profile photo update failed", e.getMessage()));
        }
    }
    @PostMapping("/address")
    public ResponseEntity<ApiResponse<Set<DeliveryAddresses>>> addAddress(
            @Valid @RequestBody DeliveryAddresses deliveryAddress,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Consumer consumer = consumerService.findByEmail(email);

            if (consumer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Add address failed", "Consumer not found"));
            }

            ApiResponse<Set<DeliveryAddresses>> response =
                    consumerService.AddDeliveryAddress(deliveryAddress, consumer.getConsumerId());

            if (response.getData() != null) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Add address failed", e.getMessage()));
        }
    }

    @PutMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<DeliveryAddresses>> updateAddress(
            @Valid @RequestBody DeliveryAddresses address,
            @PathVariable("addressId") int addressId,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Consumer consumer = consumerService.findByEmail(email);

            if (consumer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Update address failed", "Consumer not found"));
            }

            ApiResponse<DeliveryAddresses> response =
                    consumerService.UpdateDeliveryAddress(address, consumer.getConsumerId(), addressId);

            if (response.getData() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Update address failed", e.getMessage()));
        }
    }

    @DeleteMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<Set<DeliveryAddresses>>> deleteAddress(
            @PathVariable("addressId") int addressId,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Consumer consumer = consumerService.findByEmail(email);

            if (consumer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Delete address failed", "Consumer not found"));
            }

            ApiResponse<Set<DeliveryAddresses>> response =
                    consumerService.DeleteDeliveryAddress(addressId, consumer.getConsumerId());

            if (response.getData() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Delete address failed", e.getMessage()));
        }
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<Set<DeliveryAddresses>>> getAllAddresses(
            Authentication authentication) {

        try {
            String email = authentication.getName();
            Consumer consumer = consumerService.findByEmail(email);

            if (consumer == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Get addresses failed", "Consumer not found"));
            }

            ApiResponse<Set<DeliveryAddresses>> response =
                    consumerService.getDeliveryAddresses(consumer.getConsumerId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Get addresses failed", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Consumer>> getConsumerProfile(
            Authentication authentication) {

        try {
            String email = authentication.getName();
            ApiResponse<Consumer> response = consumerService.getConsumerByEmail(email);

            if (response.getData() != null) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Get profile failed", e.getMessage()));
        }
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> exceptionHandler(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication failed", "Invalid credentials"));
    }

}
