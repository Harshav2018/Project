package com.example.finalyearproject.Configs;

import com.example.finalyearproject.Utility.ApiResponse;
import com.example.finalyearproject.customExceptions.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles @Valid failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errorMessages = new ArrayList<>();
        Map<String, String> errorDetails = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errorDetails.put(error.getField(), error.getDefaultMessage());
            errorMessages.add(error.getField() + ": " + error.getDefaultMessage());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>("Validation failed", errorDetails, errorMessages));
    }

    // Handles @Validated violations (e.g., from query params)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errorMessages = new ArrayList<>();
        Map<String, String> errorDetails = new HashMap<>();

        ex.getConstraintViolations().forEach(cv -> {
            String field = cv.getPropertyPath().toString();
            errorDetails.put(field, cv.getMessage());
            errorMessages.add(field + ": " + cv.getMessage());
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>("Validation constraints violated", errorDetails, errorMessages));
    }

    // Handle ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Resource not found", ex.getMessage()));
    }

    // Catch-all exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllOtherExceptions(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", ex.getMessage()));
    }
}