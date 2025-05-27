package com.example.finalyearproject.Controllers;

import com.example.finalyearproject.DataStore.Product;
import com.example.finalyearproject.DataStore.ProductImage;
import com.example.finalyearproject.Services.ProductImageService;
import com.example.finalyearproject.Services.ProductService;
import com.example.finalyearproject.Utility.ApiResponse;
import com.example.finalyearproject.Utility.ProductFilterDTO;
import com.example.finalyearproject.Utility.ProductUpdateDTO;
import com.example.finalyearproject.Utility.ProductUtility;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductImageService productImageService;

//    @GetMapping("products/farmer/{farmerId}")
//    public ResponseEntity<ApiResponse<List<Product>>> getProductsByFarmer(@PathVariable int farmerId) {
//        ApiResponse<List<Product>> response = productService.getProductsByFarmerId(farmerId);
//        return ResponseEntity.ok(response);
//    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('FARMER')")
    public ResponseEntity<ApiResponse<Product>> addProduct(
            @ModelAttribute @Valid ProductUtility productUtility,
            Authentication authentication) {

        String farmerEmail = authentication.getName();
        ApiResponse<Product> response = productService.AddProduct(productUtility, farmerEmail);

        if (response.getData() != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAuthority('FARMER')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable int productId,
            Authentication authentication) {

        String farmerEmail = authentication.getName();
        ApiResponse<Void> response = productService.DeleteProduct(productId, farmerEmail);

        if (response.getErrors() == null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAuthority('FARMER')")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable int productId,
            @RequestBody @Valid ProductUpdateDTO productUpdateDTO,
            Authentication authentication) {

        String farmerEmail = authentication.getName();
        ApiResponse<Product> response = productService.updateProduct(productUpdateDTO, productId, farmerEmail);

        if (response.getData() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


    @PostMapping(value = "/images/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('FARMER')")
    public ResponseEntity<ApiResponse<List<ProductImage>>> addProductImages(
            @PathVariable int productId,
            @RequestParam("images") MultipartFile[] images,
            Authentication authentication) {

        String farmerEmail = authentication.getName();

        // First verify product belongs to this farmer
        ApiResponse<Product> productResponse = productService.getProductByIdAndFarmerEmail(productId, farmerEmail);

        if (productResponse.getData() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied", "You don't have permission to modify this product"));
        }

        ApiResponse<List<ProductImage>> response = productImageService.uploadProductImages(productId, images);

        if (response.getData() != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Delete a specific product image (farmer only)
     */
    @DeleteMapping("images/{imageId}")
    @PreAuthorize("hasAuthority('FARMER')")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @PathVariable int imageId,
            Authentication authentication) {

        String farmerEmail = authentication.getName();

        // First verify image belongs to this farmer's product
        ApiResponse<Boolean> ownershipResponse = productImageService.verifyImageOwnership(imageId, farmerEmail);

        if (ownershipResponse.getData() == null || !ownershipResponse.getData()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied", "You don't have permission to delete this image"));
        }

        ApiResponse<Void> response = productImageService.deleteProductImage(imageId);

        if (response.getErrors() == null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<Product>>> filterProducts(ProductFilterDTO filterDTO) {
        ApiResponse<Page<Product>> response = productService.getFilteredProducts(filterDTO);
        if (response.getData() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/public/products/random")
    public ResponseEntity<ApiResponse<Page<Product>>> getRandomProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        ApiResponse<Page<Product>> response = productService.getRandomProductsPaginated(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get random products for home page (consumer only) with pagination
     */
    @GetMapping("/consumer/products/random")
    @PreAuthorize("hasAuthority('CONSUMER')")
    public ResponseEntity<ApiResponse<Page<Product>>> getRandomProductsForConsumer(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        ApiResponse<Page<Product>> response = productService.getRandomProductsPaginated(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Reset the random ordering (useful for forcing a new shuffle)
     */
    @PostMapping("/products/random/reset")
    public ResponseEntity<ApiResponse<String>> resetRandomOrder() {
        ApiResponse<String> response = productService.resetRandomOrder();
        return ResponseEntity.ok(response);
    }
}